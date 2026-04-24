package com.concertticketing.domain.queue.controller;

import com.concertticketing.domain.queue.dto.QueueEnterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class QueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 대기열_입장_성공() throws Exception {
        QueueEnterRequest request = new QueueEnterRequest(1L);

        mockMvc.perform(post("/queue/enter")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.queueToken").value("550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(jsonPath("$.rank").value(3842))
                .andExpect(jsonPath("$.estimatedWaitSeconds").value(192));
    }

    @Test
    void 대기열_순번_조회_성공() throws Exception {
        mockMvc.perform(get("/queue/status")
                        .header("Authorization", "Bearer test-token")
                        .param("queueToken", "550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.rank").value(120))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }
}
