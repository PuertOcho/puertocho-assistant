package com.intentmanagerms.domain.repository;

import com.intentmanagerms.domain.model.ConversationState;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para gestionar el estado de conversaciones en Redis.
 */
@Repository
public interface ConversationRepository extends CrudRepository<ConversationState, String> {
    
    /**
     * Busca una conversación por sessionId.
     * @param sessionId ID de la sesión
     * @return Optional con el estado de conversación si existe
     */
    Optional<ConversationState> findBySessionId(String sessionId);
    
    /**
     * Verifica si existe una conversación activa para la sesión.
     * @param sessionId ID de la sesión
     * @return true si existe una conversación activa
     */
    boolean existsBySessionId(String sessionId);
    
    /**
     * Elimina una conversación por sessionId.
     * @param sessionId ID de la sesión
     */
    void deleteBySessionId(String sessionId);
} 