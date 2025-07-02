package com.intentmanagerms.application.services.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class NluEntity {
    @JsonProperty("entity")
    private String entity;
    
    @JsonProperty("start")
    private int start;
    
    @JsonProperty("end")
    private int end;
    
    @JsonProperty("confidence_entity")
    private String confidenceEntity;
    
    @JsonProperty("value")
    private String value;
    
    @JsonProperty("extractor")
    private String extractor;
    
    @JsonProperty("processors")
    private List<String> processors;
    
    // Constructores
    public NluEntity() {}
    
    public NluEntity(String entity, int start, int end, String confidenceEntity, 
                    String value, String extractor, List<String> processors) {
        this.entity = entity;
        this.start = start;
        this.end = end;
        this.confidenceEntity = confidenceEntity;
        this.value = value;
        this.extractor = extractor;
        this.processors = processors;
    }
    
    // Getters y Setters
    public String getEntity() {
        return entity;
    }
    
    public void setEntity(String entity) {
        this.entity = entity;
    }
    
    public int getStart() {
        return start;
    }
    
    public void setStart(int start) {
        this.start = start;
    }
    
    public int getEnd() {
        return end;
    }
    
    public void setEnd(int end) {
        this.end = end;
    }
    
    public String getConfidenceEntity() {
        return confidenceEntity;
    }
    
    public void setConfidenceEntity(String confidenceEntity) {
        this.confidenceEntity = confidenceEntity;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getExtractor() {
        return extractor;
    }
    
    public void setExtractor(String extractor) {
        this.extractor = extractor;
    }
    
    public List<String> getProcessors() {
        return processors;
    }
    
    public void setProcessors(List<String> processors) {
        this.processors = processors;
    }
    
    // Método helper para obtener confianza como double
    public double getConfidenceAsDouble() {
        try {
            return Double.parseDouble(confidenceEntity);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    @Override
    public String toString() {
        return "NluEntity{" +
                "entity='" + entity + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", confidenceEntity='" + confidenceEntity + '\'' +
                ", value='" + value + '\'' +
                ", extractor='" + extractor + '\'' +
                ", processors=" + processors +
                '}';
    }
} 