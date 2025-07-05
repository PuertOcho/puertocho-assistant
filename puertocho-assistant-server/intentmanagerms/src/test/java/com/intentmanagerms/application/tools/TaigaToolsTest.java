package com.intentmanagerms.application.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TaigaToolsTest {

    private TaigaTools taigaTools;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        // Crear RestTemplate manual para usar MockRestServiceServer
        RestTemplate restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
        taigaTools = new TaigaTools("http://localhost:5007", "user", "pass");
        // Reemplazar restTemplate interno por el mockeado
        ReflectionTestUtils.setField(taigaTools, "restTemplate", restTemplate);
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void loginYListarProyectos() {
        // Mock login
        mockServer.expect(once(), requestTo("http://localhost:5007/login"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{" +
                        "\"session_id\":\"sid-123\"" +
                        "}", MediaType.APPLICATION_JSON));

        // Mock list projects
        String projectsJson = "{" +
                "\"projects\":[{" +
                "\"id\":1,\"name\":\"Demo\"}]}";

        mockServer.expect(once(), requestTo("http://localhost:5007/projects?session_id=sid-123"))
                .andRespond(withSuccess(projectsJson, MediaType.APPLICATION_JSON));

        String result = taigaTools.listarProyectosDisponiblesTaiga();
        assertTrue(result.contains("Demo"));

        mockServer.verify();
    }
} 