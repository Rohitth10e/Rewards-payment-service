# 03 — Kafka and reliable processing

This note introduces the Apache Kafka concepts used by the service and explains why retries, idempotency, and dead-letter topics are necessary.

## 1. Kafka vocabulary

- **Producer:** publishes records.
- **Consumer:** reads records.
- **Topic:** named stream of records.
- **Partition:** ordered append-only segment of a topic.
- **Offset:** a consumer’s position in a partition.
- **Consumer group:** cooperating consumers that divide partitions.
- **Key:** value used to choose a partition and often represent an ordering identity.

This project publishes to `txn.requested` with three partitions. The consumer belongs to `rewards-group`.

## 2. Producer flow

`TransactionProducer` uses `KafkaTemplate` to publish a message. The account ID is the Kafka key:

```java
kafkaTemplate.send(TOPIC, transactionRequest.accountId(), payload);
```

Using the account ID as the key makes all events for that account eligible for the same partition, preserving their order within that partition.

Kafka ordering is only guaranteed within one partition. It does not globally order all transactions.

## 3. Consumer flow

`@KafkaListener` registers `TransactionConsumer.processTransaction` as a listener for `txn.requested`.

The consumer:

1. receives a record;
2. extracts the business data;
3. inserts a ledger entry;
4. updates the account; and
5. acknowledges successful processing according to the listener container configuration.

A consumer can receive a record again after a crash or rebalance. Therefore, consumers should be safe to run more than once.

## 4. Serialization and event contracts

The producer uses a string key serializer and Spring’s JSON value serializer. The consumer uses JSON deserialization.

An event contract should be explicit and versionable. For example:

```json
{
  "eventVersion": 1,
  "idempotencyKey": "demo-transaction-001",
  "accountId": "acc_123",
  "amount": "50.00"
}
```

The current producer places `accountId` and `amount` inside a nested `transactionRequest`, while the consumer reads them as top-level fields. This mismatch must be corrected before treating the flow as end-to-end functional. A typed `TransactionEvent` is safer than an untyped `Map<String, Object>`.

## 5. At-least-once delivery

Most practical Kafka consumers are designed around at-least-once delivery: a message may be delivered again, but successful processing should not silently lose it.

At-least-once delivery creates duplicates during:

- consumer crashes after database commit but before offset commit;
- network timeouts;
- consumer rebalances; and
- client retries.

The database unique idempotency key is the business safeguard for this service.

## 6. Idempotency

An idempotent operation produces the same business result when repeated. The key must identify the business operation, not merely an HTTP attempt.

Good key behavior:

```text
same checkout operation -> same key
different checkout operation -> different key
```

Do not generate a new key for every automatic retry if the retry represents the same operation.

## 7. Retry and dead-letter handling

`KafkaConfig` creates a `DefaultErrorHandler` with a fixed one-second delay and three retries. If processing still fails, `DeadLetterPublishingRecoverer` publishes the record to a dead-letter topic.

Retries are suitable for transient errors such as a temporary database outage. They are wasteful for permanent errors such as an invalid event schema. Production systems should classify exceptions and use different policies.

A dead-letter topic is not a trash can. Operators should:

- monitor its depth;
- retain the original failure context;
- inspect and repair invalid events safely;
- replay only after understanding the cause; and
- alert when failures exceed an agreed threshold.

## 8. Poison pills

A poison pill is a message that repeatedly crashes processing. The consumer contains an intentional `poison_pill` account ID path to exercise retries and dead-letter behavior.

This is useful for learning, but production code should replace deliberate `System.out` output with structured logs and metrics.

## 9. Kafka versus direct REST

Direct REST is appropriate when the caller needs an immediate result and the operation is short. Kafka is appropriate when work can be asynchronous, producers and consumers should be decoupled, or traffic needs buffering.

Kafka adds operational complexity:

- broker health and storage;
- consumer lag;
- partition sizing;
- schema evolution;
- retries and replay;
- security; and
- monitoring.

Do not use Kafka merely because it is popular. Use it when its delivery and decoupling properties solve a real problem.

## 10. Reliable publication: the outbox pattern

The current flow publishes to Kafka before the consumer writes PostgreSQL. In more complex systems, another common problem occurs when a service updates its database and then crashes before publishing an event.

The transactional outbox pattern solves that gap:

1. write business state and an outbox row in one database transaction;
2. run a publisher that reads unsent outbox rows;
3. publish them to Kafka;
4. mark them sent; and
5. make publication idempotent.

This adds a table and publisher but gives a clearer database-to-event delivery guarantee.

## 11. Security and configuration

The local compose setup uses plaintext Kafka and development credentials. Production Kafka should normally use TLS and authentication such as SASL, with credentials supplied by a secret manager.

The consumer configuration currently trusts all JSON packages. Restrict trusted packages and use an explicit event type in a real deployment.

## 12. Observability checklist

Track at least:

- records published and failed;
- consumer processing success/failure;
- consumer lag;
- retry count;
- dead-letter count;
- processing latency;
- duplicate idempotency detections; and
- database transaction failures.

Include a correlation ID in logs and event headers so one request can be followed across HTTP, Kafka, and PostgreSQL.

## Suggested practice exercise

Create a typed `TransactionEvent`, align producer and consumer serialization, then add a test that publishes a duplicate event and verifies that the ledger contains only one entry.
