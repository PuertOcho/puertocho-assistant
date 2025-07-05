package com.intentmanagerms.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentmanagerms.application.services.SmartAssistantService;
import com.intentmanagerms.application.services.TtsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AssistantController.class, properties = {"SPRING_APPLICATION_NAME=test","CONFIG_SERVICE=disabled","PORT_CONFIG=0","server.port=0","PORT_INTENT_MANAGER=0"})
class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SmartAssistantService smartAssistantService;
    @MockBean
    private TtsService ttsService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void chatEndpointDevuelve200() throws Exception {
        Mockito.when(smartAssistantService.chatWithSession(any(), any())).thenReturn("Hola mundo");

        String body = mapper.writeValueAsString(new AssistantController.ChatRequest("Hola", null, false, null, null, null));

        mockMvc.perform(post("/api/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Hola mundo"));
    }
} 