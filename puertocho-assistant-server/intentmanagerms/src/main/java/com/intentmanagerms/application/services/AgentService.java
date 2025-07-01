package com.intentmanagerms.application.services;

import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private final Assistant assistant;

    public AgentService(Assistant assistant) {
        this.assistant = assistant;
    }

    public String executeAction(String prompt) {
        return assistant.chat(prompt);
    }
} 