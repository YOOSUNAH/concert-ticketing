package com.concertticketing.domain.payment.controller;

import com.concertticketing.domain.payment.dto.PaymentConfirmRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 결제_확정_성공() throws Exception {
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                1L, "paykey_123", "order_abc", 237000, 5000, "CARD"
        );

        mockMvc.perform(post("/payments/confirm")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookingNumber").value("BK20250801001"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.seats").isArray())
                .andExpect(jsonPath("$.seats[0]").value("A-1"))
                .andExpect(jsonPath("$.seats[1]").value("A-2"))
                .andExpect(jsonPath("$.concertTitle").value("10cm 콘서트"))
                .andExpect(jsonPath("$.date").value("2025-08-01"))
                .andExpect(jsonPath("$.time").value("19:00"))
                .andExpect(jsonPath("$.paidAmount").value(237000))
                .andExpect(jsonPath("$.paidAt").value("2025-08-01T18:30:00"));
    }
}
