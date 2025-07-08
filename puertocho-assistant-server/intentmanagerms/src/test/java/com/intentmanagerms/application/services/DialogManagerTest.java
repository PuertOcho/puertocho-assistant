package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.ConversationState;
import com.intentmanagerms.domain.repository.ConversationRepository;
import com.intentmanagerms.application.services.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DialogManagerTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private NluService nluService;

    @InjectMocks
    private DialogManager dialogManager;

    private Map<String, ConversationState> store;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        store = new HashMap<>();

        // Simular comportamiento del repositorio en memoria
        when(conversationRepository.findBySessionId(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));

        when(conversationRepository.save(any()))
                .thenAnswer(invocation -> {
                    ConversationState cs = invocation.getArgument(0);
                    store.put(cs.getSessionId(), cs);
                    return cs;
                });
    }

    private IntentMessage buildIntentMessage(String intentName, double confidence, Map<String, String> entities) {
        IntentInfo intent = new IntentInfo(intentName, String.valueOf(confidence));
        List<EntityInfo> entityList = new ArrayList<>();
        entities.forEach((k, v) -> entityList.add(new EntityInfo(k, 0, 0, "1.0", v, "manual", List.of())));
        return new IntentMessage(intent, entityList, List.of(), "test");
    }

    @Test
    void dadoMensajeConBajaConfianza_retornaClarificacion() {
        when(nluService.analyzeText("hola"))
                .thenReturn(buildIntentMessage("saludo", 0.1, Map.of()));

        DialogResult result = dialogManager.processMessage("hola", null);

        assertTrue(result.needsClarification());
    }

    @Test
    void dadoMensajeSinEntidad_retornaFollowUp() {
        when(nluService.analyzeText("enciende la luz"))
                .thenReturn(buildIntentMessage("encender_luz", 0.9, Map.of()));

        DialogResult result = dialogManager.processMessage("enciende la luz", null);

        assertTrue(result.isFollowUp());
        assertNotNull(result.getSessionId());
    }

    @Test
    void dadoSegundaEntradaConEntidadCompleta_retornaActionReady() {
        // Primera vuelta, falta entidad "lugar"
        when(nluService.analyzeText("enciende la luz"))
                .thenReturn(buildIntentMessage("encender_luz", 0.9, Map.of()));
        DialogResult first = dialogManager.processMessage("enciende la luz", null);
        String sessionId = first.getSessionId();

        // Segunda vuelta con entidad requerida
        when(nluService.analyzeText("en el salón"))
                .thenReturn(buildIntentMessage("encender_luz", 0.9, Map.of("lugar", "salón")));
        DialogResult second = dialogManager.processMessage("en el salón", sessionId);

        assertTrue(second.isActionReady());
        assertEquals("encender_luz", second.getIntent());
        assertEquals("salón", second.getEntities().get("lugar"));
    }
} 