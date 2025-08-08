# Whisper Transcription Microservice

Un microservicio de transcripción de audio de alta precisión que combina modelos locales de OpenAI Whisper con la API externa de OpenAI para proporcionar transcripciones robustas y configurables.

## 🚀 Características Principales

- **Transcripción Dual**: Soporte para modelos locales y API externa de OpenAI
- **Configuración Flexible**: Método por defecto configurable con fallback automático
- **Alta Precisión**: Utiliza Whisper-1 de OpenAI para máxima calidad
- **Robustez**: Sistema de fallback automático para alta disponibilidad
- **GPU Acelerado**: Soporte completo para CUDA con PyTorch
- **API REST**: Interfaz HTTP simple y eficiente
- **Debug Integrado**: Sistema de logging y archivos de debug
- **Docker Optimizado**: Contenedor con todas las dependencias preinstaladas

## 📋 Tabla de Contenidos

- [Instalación](#instalación)
- [Configuración](#configuración)
- [Uso del API](#uso-del-api)
- [Configuraciones Avanzadas](#configuraciones-avanzadas)
- [Endpoints Disponibles](#endpoints-disponibles)
- [Ejemplos de Uso](#ejemplos-de-uso)
- [Monitoreo y Debug](#monitoreo-y-debug)
- [Troubleshooting](#troubleshooting)
- [Referencia de API](#referencia-de-api)

## 🛠️ Instalación

### Requisitos del Sistema

- **Docker** 20.10+
- **Docker Compose** 2.0+
- **NVIDIA Container Toolkit** (para GPU)
- **GPU NVIDIA** con soporte CUDA 11.8+ (recomendado)

### Instalación Rápida

```bash
# Clonar el repositorio
git clone <repository-url>
cd puertocho-assistant-server/whisper-ms

# Configurar variables de entorno
cp env.example env
# Editar archivo env con tus configuraciones

# Iniciar el servicio
docker-compose up -d --build
```

### Verificación de Instalación

```bash
# Verificar estado del servicio
curl http://localhost:5000/health

# Verificar configuración
curl http://localhost:5000/status | jq .
```

## ⚙️ Configuración

### Variables de Entorno Principales

```bash
# Configuración básica
WHISPER_MODEL=base              # Modelo local: tiny, base, small, medium, large
DEFAULT_LANGUAGE=es             # Idioma por defecto
DEBUG_AUDIO=false               # Habilitar debug de audio

# Configuración de API externa
ENABLE_EXTERNAL_API=true        # Habilitar API externa de OpenAI
EXTERNAL_API_KEY=your_api_key   # API key de OpenAI
EXTERNAL_API_MODEL=whisper-1    # Modelo de OpenAI

# Configuración de método y fallback
DEFAULT_TRANSCRIPTION_METHOD=external  # Método por defecto: external/local
FALLBACK_METHOD=local                  # Método de fallback
FALLBACK_ENABLED=true                  # Habilitar fallback automático
```

### Configuraciones Recomendadas

#### Para Producción (Máxima Precisión)
```bash
DEFAULT_TRANSCRIPTION_METHOD=external
FALLBACK_METHOD=local
FALLBACK_ENABLED=true
ENABLE_EXTERNAL_API=true
```

#### Para Desarrollo (Máxima Velocidad)
```bash
DEFAULT_TRANSCRIPTION_METHOD=local
FALLBACK_METHOD=external
FALLBACK_ENABLED=true
ENABLE_EXTERNAL_API=true
```

#### Para Entornos Offline
```bash
DEFAULT_TRANSCRIPTION_METHOD=local
FALLBACK_METHOD=local
FALLBACK_ENABLED=false
ENABLE_EXTERNAL_API=false
```

## 📡 Uso del API

### Transcripción Básica

```bash
# Transcripción con método por defecto configurado
curl -X POST \
  -F "audio=@audio.wav" \
  -F "language=es" \
  http://localhost:5000/transcribe
```

### Transcripción con Método Específico

```bash
# Forzar transcripción externa
curl -X POST \
  -F "audio=@audio.wav" \
  -F "language=es" \
  -F "use_external=true" \
  http://localhost:5000/transcribe

# Forzar transcripción local
curl -X POST \
  -F "audio=@audio.wav" \
  -F "language=es" \
  -F "use_external=false" \
  http://localhost:5000/transcribe
```

### Endpoints Específicos

```bash
# Transcripción local exclusiva
curl -X POST \
  -F "audio=@audio.wav" \
  -F "language=es" \
  http://localhost:5000/transcribe/local

# Transcripción externa exclusiva
curl -X POST \
  -F "audio=@audio.wav" \
  -F "language=es" \
  http://localhost:5000/transcribe/external
```

## 🔧 Configuraciones Avanzadas

### Cambiar Método por Defecto

1. **Editar configuración**:
```bash
# Para usar local por defecto
DEFAULT_TRANSCRIPTION_METHOD=local
FALLBACK_METHOD=external
```

2. **Reiniciar servicio**:
```bash
docker-compose down
docker-compose up -d --build
```

3. **Verificar cambios**:
```bash
curl http://localhost:5000/status | jq .
```

### Configuración de Modelos

| Modelo Local | Tamaño | VRAM | Velocidad | Precisión |
|--------------|--------|------|-----------|-----------|
| `tiny` | 39 MB | ~1GB | ⚡⚡⚡ | ⭐⭐ |
| `base` | 74 MB | ~1GB | ⚡⚡ | ⭐⭐⭐ |
| `small` | 244 MB | ~2GB | ⚡ | ⭐⭐⭐⭐ |
| `medium` | 769 MB | ~5GB | 🐌 | ⭐⭐⭐⭐⭐ |
| `large` | 1550 MB | ~10GB | 🐌🐌 | ⭐⭐⭐⭐⭐ |

**Nota**: La API externa siempre usa Whisper-1, que es equivalente al modelo `large` en precisión.

## 📋 Endpoints Disponibles

### Endpoints de Transcripción

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `/transcribe` | POST | Transcripción con método configurado |
| `/transcribe/local` | POST | Transcripción local exclusiva |
| `/transcribe/external` | POST | Transcripción externa exclusiva |

### Endpoints de Sistema

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `/health` | GET | Health check del servicio |
| `/status` | GET | Estado y configuración del servicio |
| `/debug/audio` | GET | Listar archivos de debug |
| `/debug/audio/<filename>` | GET | Descargar archivo de debug |

## 💻 Ejemplos de Uso

### Python

```python
import requests

def transcribe_audio(audio_file, language="es", use_external=None):
    """
    Transcribir audio usando el microservicio
    
    Args:
        audio_file (str): Ruta al archivo de audio
        language (str): Idioma del audio
        use_external (bool): Forzar método (None = usar configuración)
    """
    url = "http://localhost:5000/transcribe"
    
    with open(audio_file, 'rb') as f:
        files = {"audio": f}
        data = {"language": language}
        
        if use_external is not None:
            data["use_external"] = str(use_external).lower()
        
        response = requests.post(url, files=files, data=data)
        return response.json()

# Ejemplos de uso
result = transcribe_audio("audio.wav")  # Método por defecto
result = transcribe_audio("audio.wav", use_external=True)  # Forzar externa
result = transcribe_audio("audio.wav", use_external=False)  # Forzar local
```

### JavaScript/Node.js

```javascript
const FormData = require('form-data');
const fs = require('fs');
const axios = require('axios');

async function transcribeAudio(audioFile, language = 'es', useExternal = null) {
    const form = new FormData();
    form.append('audio', fs.createReadStream(audioFile));
    form.append('language', language);
    
    if (useExternal !== null) {
        form.append('use_external', useExternal.toString());
    }
    
    const response = await axios.post('http://localhost:5000/transcribe', form, {
        headers: form.getHeaders()
    });
    
    return response.data;
}

// Ejemplos de uso
const result = await transcribeAudio('audio.wav');
const externalResult = await transcribeAudio('audio.wav', 'es', true);
const localResult = await transcribeAudio('audio.wav', 'es', false);
```

### cURL Avanzado

```bash
# Transcripción con headers personalizados
curl -X POST \
  -H "Content-Type: multipart/form-data" \
  -F "audio=@audio.wav" \
  -F "language=es" \
  -F "use_external=true" \
  http://localhost:5000/transcribe

# Transcripción con timeout extendido
curl -X POST \
  --max-time 120 \
  -F "audio=@audio.wav" \
  -F "language=es" \
  http://localhost:5000/transcribe
```

## 📊 Monitoreo y Debug

### Verificar Estado del Servicio

```bash
# Health check básico
curl http://localhost:5000/health

# Estado detallado del servicio
curl http://localhost:5000/status | jq .

# Logs del contenedor
docker logs whisper-api --tail 50
```

### Sistema de Debug

```bash
# Habilitar debug de audio
DEBUG_AUDIO=true

# Listar archivos de debug
curl http://localhost:5000/debug/audio

# Descargar archivo de debug específico
curl http://localhost:5000/debug/audio/audio_20241220_143055_123.wav -o debug.wav
```

### Métricas de Rendimiento

```bash
# Verificar uso de GPU
docker exec whisper-api nvidia-smi

# Estadísticas del contenedor
docker stats whisper-api --no-stream
```

## 🔍 Troubleshooting

### Problemas Comunes

#### Error: "API externa no habilitada"
```bash
# Verificar configuración
curl http://localhost:5000/status | jq '.external_api_enabled'

# Habilitar API externa
ENABLE_EXTERNAL_API=true
EXTERNAL_API_KEY=your_api_key
```

#### Error: "CUDA not available"
```bash
# Verificar instalación de NVIDIA Container Toolkit
sudo apt-get install nvidia-container-toolkit
sudo systemctl restart docker

# Verificar drivers NVIDIA
nvidia-smi
```

#### Error: "Audio file is too short"
```bash
# La API externa requiere mínimo 0.1 segundos
# Usar transcripción local para audio corto
curl -X POST \
  -F "audio=@audio.wav" \
  -F "language=es" \
  -F "use_external=false" \
  http://localhost:5000/transcribe
```

#### Error: "Modelo local no disponible"
```bash
# Verificar que el modelo se cargó correctamente
docker logs whisper-api | grep "cargando modelo"

# Verificar espacio en disco
df -h
```

### Logs y Debugging

```bash
# Ver logs en tiempo real
docker logs -f whisper-api

# Ver logs de errores
docker logs whisper-api 2>&1 | grep ERROR

# Ver logs de transcripción
docker logs whisper-api | grep "Transcribiendo"
```

## 📚 Referencia de API

### Respuesta de Transcripción

```json
{
  "transcription": "Enciende luz verde",
  "language": "es",
  "detected_language": "es",
  "method": "external",
  "debug_audio_file": "audio_20241220_143055_123.wav",
  "debug_audio_url": "/debug/audio/audio_20241220_143055_123.wav"
}
```

### Respuesta con Fallback

```json
{
  "transcription": "Enciende el uverde",
  "language": "es",
  "detected_language": "es",
  "method": "local",
  "fallback": true,
  "fallback_reason": "API externa no disponible",
  "original_method": "external",
  "debug_audio_file": "audio_20241220_143055_123.wav",
  "debug_audio_url": "/debug/audio/audio_20241220_143055_123.wav"
}
```

### Respuesta de Status

```json
{
  "status": "ok",
  "device": "cuda",
  "local_model": "base",
  "default_language": "es",
  "debug_audio": false,
  "external_api_enabled": true,
  "default_transcription_method": "external",
  "fallback_method": "local",
  "fallback_enabled": true,
  "external_api_url": "https://api.openai.com/v1/audio/transcriptions",
  "external_api_model": "whisper-1",
  "external_api_configured": true
}
```

## 📄 Licencia

Este proyecto utiliza las siguientes tecnologías:
- **OpenAI Whisper**: MIT License
- **PyTorch**: BSD License
- **Flask**: BSD License

---

**Desarrollado con ❤️ para el ecosistema puertocho-assistant** 