package com.intentmanagerms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Intent Manager Microservice Application
 * 
 * LLM-RAG + MoE Architecture for Intent Classification and Conversation Management
 * 
 * Features:
 * - RAG-based intent classification with vector embeddings
 * - Mixture of Experts (MoE) voting system for improved accuracy
 * - Dynamic subtask decomposition for complex requests
 * - Redis-based conversation state management
 * - MCP integration for external services
 * 
 * @version 2.0.0-LLM-RAG
 * @author PuertoCho Assistant Team
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableRedisRepositories
@EnableAsync
@EnableScheduling
public class IntentManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntentManagerApplication.class, args);
    }
}