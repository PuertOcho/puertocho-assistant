package com.intentmanagerms.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuración de Redis para gestión de conversaciones y cache.
 * 
 * Proporciona beans para:
 * - RedisConnectionFactory usando Lettuce como cliente
 * - RedisTemplate configurado para serialización JSON
 * - Configuración personalizada basada en variables de entorno
 */
@Configuration
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host:redis}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    /**
     * Configura la fábrica de conexiones Redis usando Lettuce.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        logger.info("Configurando RedisConnectionFactory - host: {}, port: {}, database: {}", 
            redisHost, redisPort, redisDatabase);
        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration();
        redisConfiguration.setHostName(redisHost);
        redisConfiguration.setPort(redisPort);
        redisConfiguration.setDatabase(redisDatabase);
        
        // Solo establecer contraseña si no está vacía
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            redisConfiguration.setPassword(redisPassword);
        }

        return new LettuceConnectionFactory(redisConfiguration);
    }

                    /**
                 * Configura RedisTemplate con serialización JSON para objetos complejos.
                 */
                @Bean
                public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
                    logger.info("Configurando RedisTemplate con RedisConnectionFactory");
                    RedisTemplate<String, Object> template = new RedisTemplate<>();
                    template.setConnectionFactory(redisConnectionFactory);

                    // Configurar serializers
                    StringRedisSerializer stringSerializer = new StringRedisSerializer();
                    
                    // Crear ObjectMapper personalizado con soporte para Java 8 date/time
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.registerModule(new JavaTimeModule());
                    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    
                    GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

                    // Usar StringRedisSerializer para las claves
                    template.setKeySerializer(stringSerializer);
                    template.setHashKeySerializer(stringSerializer);

                    // Usar JSON serializer para los valores
                    template.setValueSerializer(jsonSerializer);
                    template.setHashValueSerializer(jsonSerializer);

                    // Habilitar transacciones si es necesario
                    template.setEnableTransactionSupport(true);

                    template.afterPropertiesSet();
                    return template;
                }
}