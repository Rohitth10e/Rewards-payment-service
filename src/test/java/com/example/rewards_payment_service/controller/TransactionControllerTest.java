package com.example.rewards_payment_service.controller;

import com.example.rewards_payment_service.service.TransactionProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionProducer producer;

    @Test
    void submitTransaction_ValidRequest_Returns202() throws Exception {
        String validJson = """
                {
                    "accountId": "acc_123",
                    "amount": 50.00
                }
                """;

        mockMvc.perform(post("/transactions")
                .header("Idempotency-Key", "test-key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
                .andExpect(status().isAccepted());
    }

    @Test
    void submitTransaction_NegativeAmount_Returns400() throws Exception {
        String invalidJson = """
                {
                    "accountId": "acc_123",
                    "amount": -10.00
                }
                """;

        mockMvc.perform(post("/transactions")
                .header("Idempotency-Key", "test-key-2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
