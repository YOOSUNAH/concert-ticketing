package com.concertticketing.domain.schedule.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 좌석_목록_조회_성공() throws Exception {
        mockMvc.perform(get("/schedules/1/seats")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.seats").isArray())
                .andExpect(jsonPath("$.seats[0].seatId").value(101))
                .andExpect(jsonPath("$.seats[0].seatNumber").value("A-1"))
                .andExpect(jsonPath("$.seats[0].grade").value("VIP"))
                .andExpect(jsonPath("$.seats[0].price").value(121000))
                .andExpect(jsonPath("$.seats[0].status").value("AVAILABLE"));
    }
}
