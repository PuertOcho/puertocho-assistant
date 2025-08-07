package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Modelo de dominio para metadata contextual de audio.
 * Contiene información contextual como ubicación, temperatura, dispositivo, etc.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AudioMetadata {

    /**
     * Identificador único del dispositivo que capturó el audio
     */
    @JsonProperty("device_id")
    private String deviceId;

    /**
     * Identificador del usuario que generó el audio
     */
    @JsonProperty("user_id")
    private String userId;

    /**
     * Ubicación geográfica donde se capturó el audio
     */
    @JsonProperty("location")
    private String location;

    /**
     * Temperatura ambiental en el momento de la captura
     */
    @JsonProperty("temperature")
    private String temperature;

    /**
     * Timestamp de cuando se capturó el audio
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * Tipo de dispositivo (smartphone, smart speaker, etc.)
     */
    @JsonProperty("device_type")
    private String deviceType;

    /**
     * Nivel de ruido ambiental (bajo, medio, alto)
     */
    @JsonProperty("noise_level")
    private String noiseLevel;

    /**
     * Idioma detectado o configurado en el dispositivo
     */
    @JsonProperty("language")
    private String language;

    /**
     * Metadatos adicionales personalizados
     */
    @JsonProperty("custom_metadata")
    private Map<String, Object> customMetadata;

    /**
     * Duración del audio en segundos
     */
    @JsonProperty("duration_seconds")
    private Double durationSeconds;

    /**
     * Formato del archivo de audio
     */
    @JsonProperty("format")
    private String format;

    /**
     * Sample rate del audio en Hz
     */
    @JsonProperty("sample_rate")
    private Integer sampleRate;

    /**
     * Número de canales de audio (mono=1, stereo=2)
     */
    @JsonProperty("channels")
    private Integer channels;

    /**
     * Calidad del audio (baja, media, alta)
     */
    @JsonProperty("quality")
    private String quality;

    /**
     * Constructor por defecto
     */
    public AudioMetadata() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor de conveniencia para metadata básica
     */
    public AudioMetadata(String deviceId, String userId, String location) {
        this.deviceId = deviceId;
        this.userId = userId;
        this.location = location;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor completo
     */
    public AudioMetadata(String deviceId, String userId, String location, String temperature,
                        LocalDateTime timestamp, String deviceType, String noiseLevel, String language,
                        Map<String, Object> customMetadata, Double durationSeconds, String format,
                        Integer sampleRate, Integer channels, String quality) {
        this.deviceId = deviceId;
        this.userId = userId;
        this.location = location;
        this.temperature = temperature;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.deviceType = deviceType;
        this.noiseLevel = noiseLevel;
        this.language = language;
        this.customMetadata = customMetadata;
        this.durationSeconds = durationSeconds;
        this.format = format;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.quality = quality;
    }

    // Getters y Setters
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getTemperature() { return temperature; }
    public void setTemperature(String temperature) { this.temperature = temperature; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getNoiseLevel() { return noiseLevel; }
    public void setNoiseLevel(String noiseLevel) { this.noiseLevel = noiseLevel; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Map<String, Object> getCustomMetadata() { return customMetadata; }
    public void setCustomMetadata(Map<String, Object> customMetadata) { this.customMetadata = customMetadata; }

    public Double getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Double durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Integer getSampleRate() { return sampleRate; }
    public void setSampleRate(Integer sampleRate) { this.sampleRate = sampleRate; }

    public Integer getChannels() { return channels; }
    public void setChannels(Integer channels) { this.channels = channels; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }

    /**
     * Obtiene un valor de metadata personalizada
     */
    public Optional<Object> getCustomValue(String key) {
        return Optional.ofNullable(customMetadata)
                .flatMap(metadata -> Optional.ofNullable(metadata.get(key)));
    }

    /**
     * Añade un valor de metadata personalizada
     */
    public void addCustomValue(String key, Object value) {
        if (customMetadata == null) {
            customMetadata = new HashMap<>();
        }
        customMetadata.put(key, value);
    }

    /**
     * Verifica si la metadata tiene información de ubicación
     */
    public boolean hasLocation() {
        return location != null && !location.trim().isEmpty();
    }

    /**
     * Verifica si la metadata tiene información de temperatura
     */
    public boolean hasTemperature() {
        return temperature != null && !temperature.trim().isEmpty();
    }

    /**
     * Verifica si la metadata tiene información de dispositivo
     */
    public boolean hasDeviceInfo() {
        return deviceId != null && !deviceId.trim().isEmpty();
    }

    /**
     * Obtiene un resumen de la metadata para logging
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("AudioMetadata{");
        
        if (hasDeviceInfo()) {
            summary.append("device=").append(deviceId);
        }
        if (userId != null) {
            summary.append(", user=").append(userId);
        }
        if (hasLocation()) {
            summary.append(", location=").append(location);
        }
        if (hasTemperature()) {
            summary.append(", temp=").append(temperature);
        }
        if (timestamp != null) {
            summary.append(", time=").append(timestamp);
        }
        if (durationSeconds != null) {
            summary.append(", duration=").append(durationSeconds).append("s");
        }
        if (format != null) {
            summary.append(", format=").append(format);
        }
        
        summary.append("}");
        return summary.toString();
    }

    /**
     * Crea una copia de la metadata con timestamp actualizado
     */
    public AudioMetadata withCurrentTimestamp() {
        return new AudioMetadata(
                this.deviceId,
                this.userId,
                this.location,
                this.temperature,
                LocalDateTime.now(),
                this.deviceType,
                this.noiseLevel,
                this.language,
                this.customMetadata != null ? new HashMap<>(this.customMetadata) : null,
                this.durationSeconds,
                this.format,
                this.sampleRate,
                this.channels,
                this.quality
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioMetadata that = (AudioMetadata) o;
        return Objects.equals(deviceId, that.deviceId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(location, that.location) &&
                Objects.equals(temperature, that.temperature) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(deviceType, that.deviceType) &&
                Objects.equals(noiseLevel, that.noiseLevel) &&
                Objects.equals(language, that.language) &&
                Objects.equals(customMetadata, that.customMetadata) &&
                Objects.equals(durationSeconds, that.durationSeconds) &&
                Objects.equals(format, that.format) &&
                Objects.equals(sampleRate, that.sampleRate) &&
                Objects.equals(channels, that.channels) &&
                Objects.equals(quality, that.quality);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, userId, location, temperature, timestamp, deviceType,
                noiseLevel, language, customMetadata, durationSeconds, format, sampleRate, channels, quality);
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
