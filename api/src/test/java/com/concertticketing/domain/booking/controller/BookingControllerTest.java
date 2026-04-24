package com.concertticketing.domain.booking.controller;

import com.concertticketing.domain.booking.dto.BookingCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 예매_생성_성공() throws Exception {
        BookingCreateRequest request = new BookingCreateRequest(1L, List.of(101L, 102L), "admission-token");

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookingId").value(999))
                .andExpect(jsonPath("$.totalAmount").value(242000))
                .andExpect(jsonPath("$.bookerName").value("홍길동"));
    }

    @Test
    void 예매_내역_조회_성공() throws Exception {
        mockMvc.perform(get("/bookings/me")
                        .header("Authorization", "Bearer test-token")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].bookingId").value(999))
                .andExpect(jsonPath("$.content[0].concertTitle").value("10cm 콘서트"))
                .andExpect(jsonPath("$.content[0].date").value("2025-08-01"))
                .andExpect(jsonPath("$.content[0].time").value("19:00"))
                .andExpect(jsonPath("$.content[0].seats").isArray())
                .andExpect(jsonPath("$.content[0].seats[0]").value("A-1"))
                .andExpect(jsonPath("$.content[0].seats[1]").value("A-2"))
                .andExpect(jsonPath("$.content[0].totalAmount").value(242000))
                .andExpect(jsonPath("$.content[0].status").value("PAID"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void 예매_내역_상세_조회_성공() throws Exception {
        mockMvc.perform(get("/bookings/1")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookingId").value(1))
                .andExpect(jsonPath("$.bookingNumber").value("BK20250801001"))
                .andExpect(jsonPath("$.concertTitle").value("10cm 콘서트"))
                .andExpect(jsonPath("$.venue").value("올림픽공원"))
                .andExpect(jsonPath("$.date").value("2025-08-01"))
                .andExpect(jsonPath("$.time").value("19:00"))
                .andExpect(jsonPath("$.seats[0]").value("A-1"))
                .andExpect(jsonPath("$.seats[1]").value("A-2"))
                .andExpect(jsonPath("$.totalAmount").value(242000))
                .andExpect(jsonPath("$.paidAmount").value(237000))
                .andExpect(jsonPath("$.pointUsed").value(5000))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.ticketType").value("MOBILE"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").value("2025-08-01T18:30:00"));
    }

    @Test
    void 예매_취소_성공() throws Exception {
        mockMvc.perform(delete("/bookings/1")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookingId").value(1))
                .andExpect(jsonPath("$.bookingNumber").value("BK20250801001"))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAmount").value(242000))
                .andExpect(jsonPath("$.cancelledAt").value("2025-08-01T20:00:00"));
    }
}
