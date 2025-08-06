package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Resultado del proceso de slot-filling automático.
 * Contiene información sobre slots completados, preguntas generadas y estado del proceso.
 */
public class SlotFillingResult {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("slots_completed")
    private boolean slotsCompleted;

    @JsonProperty("filled_slots")
    private Map<String, Object> filledSlots;

    @JsonProperty("missing_slots")
    private List<String> missingSlots;

    @JsonProperty("generated_question")
    private String generatedQuestion;

    @JsonProperty("next_slot_to_fill")
    private String nextSlotToFill;

    @JsonProperty("slot_extraction_confidence")
    private Map<String, Double> slotExtractionConfidence;

    @JsonProperty("overall_confidence")
    private double overallConfidence;

    @JsonProperty("requires_clarification")
    private boolean requiresClarification;

    @JsonProperty("clarification_question")
    private String clarificationQuestion;

    @JsonProperty("validation_errors")
    private List<String> validationErrors;

    @JsonProperty("suggested_values")
    private Map<String, List<String>> suggestedValues;

    @JsonProperty("processing_time_ms")
    private long processingTimeMs;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("error_message")
    private String errorMessage;

    public SlotFillingResult() {
        this.timestamp = LocalDateTime.now();
    }

    public SlotFillingResult(boolean success) {
        this();
        this.success = success;
    }

    // Getters y Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSlotsCompleted() {
        return slotsCompleted;
    }

    public void setSlotsCompleted(boolean slotsCompleted) {
        this.slotsCompleted = slotsCompleted;
    }

    public Map<String, Object> getFilledSlots() {
        return filledSlots;
    }

    public void setFilledSlots(Map<String, Object> filledSlots) {
        this.filledSlots = filledSlots;
    }

    public List<String> getMissingSlots() {
        return missingSlots;
    }

    public void setMissingSlots(List<String> missingSlots) {
        this.missingSlots = missingSlots;
    }

    public String getGeneratedQuestion() {
        return generatedQuestion;
    }

    public void setGeneratedQuestion(String generatedQuestion) {
        this.generatedQuestion = generatedQuestion;
    }

    public String getNextSlotToFill() {
        return nextSlotToFill;
    }

    public void setNextSlotToFill(String nextSlotToFill) {
        this.nextSlotToFill = nextSlotToFill;
    }

    public Map<String, Double> getSlotExtractionConfidence() {
        return slotExtractionConfidence;
    }

    public void setSlotExtractionConfidence(Map<String, Double> slotExtractionConfidence) {
        this.slotExtractionConfidence = slotExtractionConfidence;
    }

    public double getOverallConfidence() {
        return overallConfidence;
    }

    public void setOverallConfidence(double overallConfidence) {
        this.overallConfidence = overallConfidence;
    }

    public boolean isRequiresClarification() {
        return requiresClarification;
    }

    public void setRequiresClarification(boolean requiresClarification) {
        this.requiresClarification = requiresClarification;
    }

    public String getClarificationQuestion() {
        return clarificationQuestion;
    }

    public void setClarificationQuestion(String clarificationQuestion) {
        this.clarificationQuestion = clarificationQuestion;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public Map<String, List<String>> getSuggestedValues() {
        return suggestedValues;
    }

    public void setSuggestedValues(Map<String, List<String>> suggestedValues) {
        this.suggestedValues = suggestedValues;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Verifica si hay un slot específico completado
     */
    public boolean hasSlot(String slotName) {
        return filledSlots != null && filledSlots.containsKey(slotName);
    }

    /**
     * Obtiene el valor de un slot específico
     */
    public Object getSlotValue(String slotName) {
        return filledSlots != null ? filledSlots.get(slotName) : null;
    }

    /**
     * Verifica si necesita más información del usuario
     */
    public boolean needsMoreInformation() {
        return !slotsCompleted || requiresClarification;
    }

    @Override
    public String toString() {
        return "SlotFillingResult{" +
                "success=" + success +
                ", slotsCompleted=" + slotsCompleted +
                ", filledSlots=" + filledSlots +
                ", missingSlots=" + missingSlots +
                ", generatedQuestion='" + generatedQuestion + '\'' +
                ", nextSlotToFill='" + nextSlotToFill + '\'' +
                ", overallConfidence=" + overallConfidence +
                ", requiresClarification=" + requiresClarification +
                '}';
    }
}