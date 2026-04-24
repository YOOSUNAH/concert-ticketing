package com.concertticketing.domain.concert.controller;

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
class ConcertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 콘서트_목록_조회_성공() throws Exception {
        mockMvc.perform(get("/concerts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].concertId").value(1))
                .andExpect(jsonPath("$.content[0].title").value("10cm 콘서트"))
                .andExpect(jsonPath("$.content[0].artist").value("10cm"))
                .andExpect(jsonPath("$.content[0].thumbnailUrl").isString())
                .andExpect(jsonPath("$.content[0].venue").value("올림픽공원"))
                .andExpect(jsonPath("$.content[0].startDate").value("2025-08-01"))
                .andExpect(jsonPath("$.content[0].endDate").value("2025-08-02"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(50))
                .andExpect(jsonPath("$.totalPages").value(5))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void 콘서트_상세_조회_성공() throws Exception {
        mockMvc.perform(get("/concerts/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.concertId").value(1))
                .andExpect(jsonPath("$.title").value("10cm 콘서트"))
                .andExpect(jsonPath("$.artist").value("10cm"))
                .andExpect(jsonPath("$.description").value("공연 설명"))
                .andExpect(jsonPath("$.venue").value("올림픽공원"))
                .andExpect(jsonPath("$.posterUrl").isString())
                .andExpect(jsonPath("$.maxTicketsPerPerson").value(2))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.schedules").isArray())
                .andExpect(jsonPath("$.schedules[0].scheduleId").value(1))
                .andExpect(jsonPath("$.schedules[0].date").value("2025-08-01"))
                .andExpect(jsonPath("$.schedules[0].time").value("19:00"))
                .andExpect(jsonPath("$.schedules[0].totalSeats").value(500))
                .andExpect(jsonPath("$.schedules[0].remainingSeats").value(120));
    }
}
