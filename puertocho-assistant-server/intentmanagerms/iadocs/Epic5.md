# Epic 5 – Integración Audio y Transcripción

**Descripción del Epic**: Integrar completamente el pipeline de audio desde recepción hasta respuesta. Reemplaza la dependencia de servicios externos de transcripción por integración directa con whisper-ms, maneja metadata de audio contextual (ubicación, temperatura, etc.), y soporta tanto entrada de texto como audio de forma unificada.

**Objetivos clave**:
- Pipeline completo: Audio → Transcripción → Clasificación → Respuesta
- Integración directa con whisper-ms para transcripción
- Soporte para metadata contextual de dispositivos (temperatura, ubicación)
- Manejo robusto de errores de transcripción
- API unificada para texto y audio

## 📋 **IMPLEMENTACIÓN REAL - EPIC 5**

### **T5.1 ✅ - AudioProcessingController**
**Descripción**: Crear `AudioProcessingController` para recibir audio multipart/form-data con metadata contextual.

**Objetivos específicos**:
- Endpoint REST para recepción de audio multipart/form-data
- Soporte para metadata contextual (ubicación, temperatura, dispositivo)
- Validación de formatos de audio soportados
- Integración con el pipeline de procesamiento existente
- Manejo de errores robusto

**Archivos Implementados**:
- ✅ `AudioProcessingController.java` - Controlador REST para audio con 5 endpoints
- ✅ `AudioProcessingService.java` - Servicio de procesamiento de audio completo
- ✅ `AudioMetadata.java` - Modelo para metadata de audio contextual
- ✅ `AudioProcessingRequest.java` - Request model para audio con configuración
- ✅ `AudioProcessingResult.java` - Response model para audio con transcripción y resultados
- ✅ `test_audio_processing.py` - Script de pruebas automatizadas completo
- ✅ `Epic5.md` - Documentación completa (este archivo)

**Configuración necesaria**:
```yaml
# Audio Processing Configuration
audio:
  processing:
    max-file-size: ${AUDIO_MAX_FILE_SIZE:10485760}
    supported-formats: ${AUDIO_SUPPORTED_FORMATS:wav,mp3,m4a,flac}
    temp-directory: ${AUDIO_TEMP_DIRECTORY:/tmp/audio}
    enable-metadata: ${AUDIO_ENABLE_METADATA:true}
    metadata-fields:
      - location
      - temperature
      - device_id
      - timestamp
      - user_id
    validation:
      max-duration-seconds: ${AUDIO_MAX_DURATION:60}
      min-duration-seconds: ${AUDIO_MIN_DURATION:0}
      sample-rate-range: ${AUDIO_SAMPLE_RATE_RANGE:8000-48000}
```

**API REST Implementada**:
```bash
POST /api/v1/audio/process              # Procesar audio con metadata
POST /api/v1/audio/process/simple       # Procesar audio simple
GET  /api/v1/audio/supported-formats    # Formatos soportados
GET  /api/v1/audio/health               # Health check
POST /api/v1/audio/test                 # Test con archivo de ejemplo
```

**Flujo de Procesamiento Implementado**:
```
1. Cliente envía audio multipart/form-data + metadata
2. AudioProcessingController valida formato y tamaño
3. AudioProcessingService procesa el audio
4. Se simula transcripción (se implementará con Whisper en T5.2)
5. Se pasa al pipeline RAG + MoE existente
6. Se retorna respuesta unificada (texto + metadata)
```

**Funcionalidades Implementadas**:
- ✅ **Validación de archivos**: Formato, tamaño, contenido
- ✅ **Metadata contextual**: Ubicación, temperatura, dispositivo, usuario
- ✅ **Configuración flexible**: Parámetros de procesamiento personalizables
- ✅ **Integración con RAG**: Clasificación de intenciones usando motor RAG
- ✅ **Integración con MoE**: Sistema de votación para mejor precisión
- ✅ **Manejo de errores**: Validación robusta y respuestas informativas
- ✅ **API REST completa**: 5 endpoints operativos
- ✅ **Pruebas automatizadas**: Script completo con 7 tipos de pruebas

---

### **T5.2 ⏳ - WhisperTranscriptionService**
**Descripción**: Implementar cliente `WhisperTranscriptionService` para whisper-ms.

**Objetivos específicos**:
- Cliente HTTP para comunicación con whisper-ms
- Manejo de timeouts y reintentos
- Soporte para diferentes formatos de audio
- Procesamiento de respuestas de transcripción
- Fallback en caso de fallo del servicio

---

### **T5.3 ⏳ - Pipeline Audio → Transcripción → Clasificación → Respuesta**
**Descripción**: Desarrollar pipeline completo integrando todos los componentes.

**Objetivos específicos**:
- Integración completa del flujo de audio
- Manejo de errores en cada etapa
- Respuestas unificadas (texto + audio)
- Optimización de rendimiento
- Logging detallado del pipeline

---

### **T5.4 ⏳ - Soporte para Metadata de Audio y Contexto de Dispositivo**
**Descripción**: Añadir soporte para metadata de audio y contexto de dispositivo.

**Objetivos específicos**:
- Modelo de metadata contextual
- Integración con el sistema de conversación
- Persistencia de contexto de dispositivo
- Enriquecimiento de prompts con contexto

---

### **T5.5 ⏳ - Manejo de Errores de Transcripción con Fallbacks**
**Descripción**: Implementar manejo de errores de transcripción con fallbacks.

**Objetivos específicos**:
- Detección de errores de transcripción
- Fallbacks automáticos
- Respuestas de error informativas
- Métricas de calidad de transcripción

---

## Variables de Entorno Clave

```bash
# Audio Processing Configuration
AUDIO_MAX_FILE_SIZE=10485760
AUDIO_SUPPORTED_FORMATS=wav,mp3,m4a,flac
AUDIO_TEMP_DIRECTORY=/tmp/audio
AUDIO_ENABLE_METADATA=true
AUDIO_MAX_DURATION=60
AUDIO_MIN_DURATION=0
AUDIO_SAMPLE_RATE_RANGE=8000-48000

# Whisper Integration
WHISPER_SERVICE_URL=http://whisper-ms:5000
WHISPER_ENABLED=true
WHISPER_TIMEOUT=30
WHISPER_MAX_RETRIES=3

# Audio Metadata Fields
AUDIO_METADATA_LOCATION_ENABLED=true
AUDIO_METADATA_TEMPERATURE_ENABLED=true
AUDIO_METADATA_DEVICE_ID_ENABLED=true
AUDIO_METADATA_TIMESTAMP_ENABLED=true
AUDIO_METADATA_USER_ID_ENABLED=true
```

## Estado del Epic 5

```
🔄 EPIC 5 - Integración Audio y Transcripción: EN PROGRESO
✅ T5.1 - AudioProcessingController: COMPLETADO
⏳ T5.2 - WhisperTranscriptionService: PENDIENTE
⏳ T5.3 - Pipeline Completo: PENDIENTE
⏳ T5.4 - Metadata Contextual: PENDIENTE
⏳ T5.5 - Manejo de Errores: PENDIENTE

📊 Progreso: 1/5 tareas completadas (20%)
```

## Integración con Epics Anteriores

- ✅ **Epic 1**: Arquitectura Base - Infraestructura lista
- ✅ **Epic 2**: Motor RAG - Clasificación de intenciones lista
- ✅ **Epic 3**: MoE Voting - Sistema de votación lista
- ✅ **Epic 4**: Sistema Conversacional - Conversación lista
- 🔄 **Epic 5**: Integración Audio - En progreso

## Casos de Uso de Audio

**Ejemplo 1 - Audio Simple**:
```
Usuario envía: audio.wav (sin metadata)
Pipeline: Audio → Whisper → "¿qué tiempo hace?" → RAG → MoE → Respuesta
```

**Ejemplo 2 - Audio con Contexto**:
```
Usuario envía: audio.wav + metadata (ubicación: "Madrid", temperatura: "22°C")
Pipeline: Audio → Whisper → "¿qué tiempo hace?" → RAG + Contexto → MoE → Respuesta contextualizada
```

**Ejemplo 3 - Audio Complejo**:
```
Usuario envía: audio.wav + metadata (dispositivo: "salón", usuario: "maria")
Pipeline: Audio → Whisper → "enciende la luz y pon música" → RAG → MoE → Descomposición → Múltiples acciones
```
