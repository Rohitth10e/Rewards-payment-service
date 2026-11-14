# 02 — PostgreSQL, JPA, and Flyway

This note explains how the service stores account state and ledger history. It connects the SQL migration to the Java entities and repositories.

## 1. Why PostgreSQL?

A payment or rewards ledger needs durable storage, constraints, transactions, and reliable numeric types. PostgreSQL provides these relational guarantees and supports strong querying for account balances and audit history.

Kafka transports events; PostgreSQL owns the current business state in this design.

## 2. The schema

The Flyway migration creates two tables:

```sql
CREATE TABLE accounts (
    id VARCHAR(50) PRIMARY KEY,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00
);
```

`accounts` stores the current balance. `ledger_entries` stores each transaction:

```sql
idempotency_key VARCHAR(255) UNIQUE NOT NULL
```

That unique constraint is a database-enforced duplicate guard. It remains effective across restarts and application replicas.

The foreign key from `ledger_entries.account_id` to `accounts.id` prevents a ledger entry from referencing an account that does not exist.

## 3. Flyway migrations

Flyway applies versioned SQL files in order. The first file is:

```text
src/main/resources/db/migration/V1__init__schema.sql
```

The naming pattern is:

```text
V<version>__<description>.sql
```

After a migration is applied, Flyway records it in its history table. Do not edit an already-applied migration in a shared environment; create `V2__...sql` for the next change.

Example future migration:

```sql
ALTER TABLE accounts ADD COLUMN currency CHAR(3) NOT NULL DEFAULT 'USD';
```

## 4. JPA entities

`Account` and `LedgerEntry` are annotated with `@Entity` and mapped to tables using `@Table`. `@Id` identifies the primary key. `LedgerEntry.id` uses database identity generation.

JPA maps Java objects to relational rows, but mapping is not a substitute for a good schema. Keep constraints in the database even when Java validation exists.

## 5. Spring Data repositories

The repositories extend `JpaRepository`:

```java
public interface AccountRepository extends JpaRepository<Account, String> {}
```

This supplies common operations such as `findById`, `save`, and `findAll` without handwritten CRUD SQL. Domain-specific query methods can be added when needed.

## 6. Transactions

`TransactionConsumer.processTransaction` is annotated with `@Transactional`. The intended unit of work is:

1. insert the ledger entry;
2. load the account;
3. update the balance;
4. commit both changes together.

If the transaction rolls back, the ledger insert and balance update should not be partially committed.

There is an important nuance in the current code: it explicitly avoids rolling back for `DataIntegrityViolationException` so duplicate idempotency keys can be handled as a normal duplicate. Transaction rollback behavior should be tested carefully because database constraint exceptions can mark a transaction rollback-only depending on the provider and timing.

## 7. Idempotency at the database layer

The desired invariant is:

```text
one idempotency key -> at most one ledger entry
```

The safe pattern is to let PostgreSQL enforce the invariant. A pre-check such as `existsByIdempotencyKey` alone is vulnerable to a race:

```text
consumer A checks: absent
consumer B checks: absent
consumer A inserts
consumer B inserts
```

A unique constraint closes that race. The application should handle the duplicate outcome explicitly and observably.

## 8. Concurrency and balance updates

The current implementation reads an account, subtracts in Java, and saves it. Two concurrent consumers can both read the same old balance and overwrite each other. Production options include:

- optimistic locking with a `@Version` column;
- pessimistic row locks;
- an atomic SQL update such as `balance = balance - :amount WHERE balance >= :amount`; or
- serialized processing per account combined with defensive database constraints.

Choose based on throughput, contention, and the business rule for insufficient funds.

## 9. Money and constraints

The schema uses `DECIMAL(15, 2)`, which avoids binary floating-point rounding. A stronger production schema might also define:

- currency code;
- a non-negative balance check;
- a positive amount check;
- transaction type (`DEBIT` or `CREDIT`);
- a reference to the originating order; and
- an immutable event timestamp.

Constraints should reflect business invariants wherever possible.

## 10. Useful database inspection commands

With the local container running:

```bash
docker exec -it <postgres-container> psql -U postgres -d rewards_db
```

Then:

```sql
\dt
SELECT * FROM accounts;
SELECT * FROM ledger_entries ORDER BY created_at DESC;
```

Use read-only queries while learning. Never run destructive SQL against a shared environment without a reviewed migration and backup plan.

## 11. Common beginner mistakes

- Editing an applied Flyway migration instead of adding a new one.
- Relying on Java validation without database constraints.
- Using `double` for money.
- Updating a balance without handling concurrent transactions.
- Treating a ledger as mutable current state instead of an audit trail.
- Assuming `save()` alone defines transaction boundaries.
- Returning entities with internal fields directly from an API.

## Suggested practice exercise

Add a `@Version` column to `Account`, write a concurrent update test, and decide whether an optimistic-lock retry or an atomic SQL update better fits the service.
