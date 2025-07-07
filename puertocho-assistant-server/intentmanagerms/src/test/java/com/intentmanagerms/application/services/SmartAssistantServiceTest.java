package com.intentmanagerms.application.services;

import com.intentmanagerms.application.services.DialogResultType;
import com.intentmanagerms.application.services.DialogResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.intentmanagerms.application.tools.SystemTools;
import com.intentmanagerms.application.tools.SmartHomeTools;
import com.intentmanagerms.application.tools.TaigaTools;

class SmartAssistantServiceTest {

    @Mock
    private DialogManager dialogManager;
    @Mock
    private SystemTools systemTools;
    @Mock
    private SmartHomeTools smartHomeTools;
    @Mock
    private TtsService ttsService;
    @Mock
    private TaigaTools taigaTools;

    private SmartAssistantService assistant;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        assistant = new SmartAssistantService(dialogManager, systemTools, smartHomeTools, ttsService, taigaTools, false, true);
    }

    @Test
    void cuandoDialogManagerDevuelveFollowUp_entregaMensajeAlUsuario() {
        when(dialogManager.processMessage(eq("hola"), any())).thenReturn(DialogResult.followUp("¿En qué puedo ayudarte?", "sid"));

        String response = assistant.chat("hola");

        assertEquals("¿En qué puedo ayudarte?", response);
    }

    @Test
    void cuandoDialogManagerDevuelveActionReady_ejecutaIntencion() {
        DialogResult dr = DialogResult.actionReady("encender_luz", Map.of("lugar", "salón"), "sid");
        when(dialogManager.processMessage(eq("enciende luz salón"), any())).thenReturn(dr);
        when(smartHomeTools.encenderLuz("salón")).thenReturn("OK. La luz ha sido encendida.");

        String response = assistant.chat("enciende luz salón");

        assertEquals("OK. La luz ha sido encendida.", response);
    }

    @Test
    void cuandoDialogManagerDevuelveError_retornaMensajeDeError() {
        when(dialogManager.processMessage(eq("algo"), any())).thenReturn(DialogResult.error("Error interno"));

        String response = assistant.chat("algo");

        assertEquals("Error interno", response);
    }

    @Test
    void cuandoAccionComplejaSinProjectId_ejecutaYDevuelveRespuestaTaiga() {
        DialogResult dr = DialogResult.actionReady("accion_compleja_taiga", java.util.Map.of(), "sid");
        when(dialogManager.processMessage(eq("Generar reporte"), any())).thenReturn(dr);
        when(taigaTools.ejecutarAccionComplejaTaiga("Generar reporte", null)).thenReturn("Reporte generado");

        String response = assistant.chat("Generar reporte");
        assertEquals("Reporte generado", response);
    }
} 