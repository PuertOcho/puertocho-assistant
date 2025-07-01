# STTms - Speech-to-Text Microservice

🎤 **Servicio de transcripción de audio de alta velocidad con aceleración por GPU**

STTms es un microservicio especializado en transcripción de audio que utiliza OpenAI Whisper, ejecutado en local y con soporte para GPU NVIDIA. Diseñado para integrarse con sistemas de asistentes de voz y aplicaciones que requieren transcripción en tiempo real con alta precisión.

## 🚀 Características principales

- ✅ **Transcripción con GPU**: Aceleración CUDA para procesamiento rápido
- ✅ **Múltiples idiomas**: Soporte para español (por defecto) y otros idiomas
- ✅ **API REST**: Interfaz HTTP simple y eficiente
- ✅ **Debug integrado**: Guardado y reproducción de audio para depuración
- ✅ **Docker optimizado**: Contenedor con PyTorch y CUDA preinstalados
- ✅ **Configuración flexible**: Variables de entorno para personalización
- ✅ **Logs detallados**: Monitoreo completo del proceso de transcripción

## 🛠️ Requisitos del sistema

### Hardware mínimo
- **GPU**: NVIDIA con soporte CUDA 11.8+ (recomendado GTX 1060 o superior)
- **RAM**: 4GB mínimo, 8GB recomendado
- **Almacenamiento**: 2GB para modelos base, 5GB para modelos grandes

### Software
- **Docker** 20.10+
- **Docker Compose** 2.0+
- **NVIDIA Container Toolkit**
- **Drivers NVIDIA** compatibles con CUDA 11.8

## 📦 Instalación

### 1. Clonar el repositorio
```bash
git clone <tu-repositorio>/STTms.git
cd STTms
```

### 2. Configurar variables de entorno
Crea un archivo `.env` en la raíz del proyecto:

```bash
# Configuración de la API
API_PORT=5000
FLASK_HOST=0.0.0.0
FLASK_PORT=5000

# Configuración de GPU
NVIDIA_VISIBLE_DEVICES=all

# Configuración de Whisper
WHISPER_MODEL=base              # tiny, base, small, medium, large
DEFAULT_LANGUAGE=es             # Idioma por defecto (español)

# Debug (opcional)
DEBUG_AUDIO=true               # Guardar audio para debug
```

### 3. Construir y ejecutar
```bash
# Construir e iniciar el servicio
docker-compose up --build

# Ejecutar en segundo plano
docker-compose up -d --build
```

## 🔧 Configuración

### Modelos disponibles
| Modelo | Tamaño | VRAM | Velocidad | Precisión |
|--------|--------|------|-----------|-----------|
| `tiny` | 39 MB | ~1GB | ⚡⚡⚡ | ⭐⭐ |
| `base` | 74 MB | ~1GB | ⚡⚡ | ⭐⭐⭐ |
| `small` | 244 MB | ~2GB | ⚡ | ⭐⭐⭐⭐ |
| `medium` | 769 MB | ~5GB | 🐌 | ⭐⭐⭐⭐⭐ |
| `large` | 1550 MB | ~10GB | 🐌🐌 | ⭐⭐⭐⭐⭐ |

### Idiomas soportados
- `es` - Español (por defecto)
- `en` - Inglés
- `fr` - Francés
- `de` - Alemán
- `it` - Italiano
- `pt` - Portugués
- Y muchos más...

## 📡 Uso del API

### Endpoint principal: `/transcribe`

**Transcripción básica (español por defecto)**
```bash
curl -X POST \
  -F "audio=@audio.wav" \
  http://localhost:5000/transcribe
```

**Especificando idioma**
```bash
curl -X POST \
  -F "audio=@audio.wav" \
  -F "language=en" \
  http://localhost:5000/transcribe
```

**Respuesta**
```json
{
  "transcription": "enciende led verde",
  "language": "es",
  "detected_language": "es",
  "debug_audio_file": "audio_20241220_143055_123.wav",
  "debug_audio_url": "/debug/audio/audio_20241220_143055_123.wav"
}
```

### Endpoints de debug

**Listar archivos de audio guardados**
```bash
curl http://localhost:5000/debug/audio
```

**Descargar archivo de audio específico**
```bash
curl http://localhost:5000/debug/audio/audio_20241220_143055_123.wav -o test.wav
```

## 🐛 Funcionalidades de debug

### Guardado automático de audio
Cuando `DEBUG_AUDIO=true`, cada audio recibido se guarda automáticamente en `./debug_audio/` con formato:
```
audio_YYYYMMDD_HHMMSS_mmm.wav
```

### Casos de uso del debug
1. **Verificar calidad del audio**: Reproduce el archivo guardado
2. **Comparar transcripciones**: Prueba el mismo audio con diferentes modelos
3. **Analizar problemas**: Identifica si el problema está en el audio o el modelo
4. **Optimización**: Ajusta configuraciones basándote en resultados reales

### Ejemplo de debug completo
```bash
# 1. Hacer transcripción
curl -X POST -F "audio=@mi_audio.wav" http://localhost:5000/transcribe

# 2. Ver archivos guardados
curl http://localhost:5000/debug/audio

# 3. Descargar para escuchar
curl http://localhost:5000/debug/audio/audio_20241220_143055_123.wav -o debug.wav

# 4. Reproducir el audio
vlc debug.wav  # o mpv debug.wav
```

## 🔍 Integración con clientes

### Python
```python
import requests

def transcribir_audio(archivo_audio, idioma="es"):
    url = "http://localhost:5000/transcribe"
    
    with open(archivo_audio, 'rb') as f:
        files = {"audio": f}
        data = {"language": idioma}
        
        response = requests.post(url, files=files, data=data)
        return response.json()

# Uso
resultado = transcribir_audio("audio.wav", "es")
print(f"Transcripción: {resultado['transcription']}")
```

### JavaScript/Node.js
```javascript
const FormData = require('form-data');
const fs = require('fs');
const axios = require('axios');

async function transcribirAudio(archivoAudio, idioma = 'es') {
    const form = new FormData();
    form.append('audio', fs.createReadStream(archivoAudio));
    form.append('language', idioma);
    
    const response = await axios.post('http://localhost:5000/transcribe', form, {
        headers: form.getHeaders()
    });
    
    return response.data;
}

// Uso
transcribirAudio('audio.wav', 'es')
    .then(resultado => console.log('Transcripción:', resultado.transcription));
```

## 📊 Monitoreo y logs

### Logs del contenedor
```bash
# Ver logs en tiempo real
docker-compose logs -f whisper-api

# Ver últimos logs
docker-compose logs --tail=100 whisper-api
```

### Información de GPU
```bash
# Verificar uso de GPU
nvidia-smi

# Ver logs de GPU en el contenedor
docker exec whisper-api nvidia-smi
```

## ⚡ Optimización de rendimiento

### Para GPU de gama baja
```bash
WHISPER_MODEL=tiny
DEBUG_AUDIO=false
```

### Para máxima precisión
```bash
WHISPER_MODEL=large
DEBUG_AUDIO=true
```

### Para balance rendimiento/precisión
```bash
WHISPER_MODEL=base  # o small
DEBUG_AUDIO=true
```

## 🚨 Troubleshooting

### Problemas comunes

**Error: CUDA not available**
```bash
# Verificar instalación de NVIDIA Container Toolkit
sudo apt-get install nvidia-container-toolkit
sudo systemctl restart docker
```

**Audio no se transcribe correctamente**
1. Verificar calidad del audio usando debug
2. Probar con modelo más grande
3. Especificar idioma correcto
4. Verificar formato de audio (preferible WAV 16kHz mono)

**Contenedor no inicia**
```bash
# Verificar logs detallados
docker-compose logs whisper-api

# Verificar recursos de GPU disponibles
nvidia-smi
```

**Respuesta lenta**
1. Usar modelo más pequeño (`tiny` o `base`)
2. Verificar que la GPU esté siendo utilizada
3. Reducir calidad del audio de entrada

## 🔒 Seguridad

- Los archivos de audio se eliminan automáticamente después de la transcripción
- Los archivos de debug se guardan localmente (no se transmiten)
- El servicio no almacena transcripciones permanentemente
- Configurar firewall para limitar acceso al puerto 5000

## 🤝 Integración con puertocho-assistant

Este servicio está diseñado para integrarse con el sistema puertocho-assistant:

```python
# Ejemplo de integración
audio_data = grabar_audio()
response = requests.post(
    "http://whisper-api:5000/transcribe",
    files={"audio": audio_data},
    data={"language": "es"}
)
comando = response.json()["transcription"]
```

## 📈 Métricas y estadísticas

El servicio proporciona información útil en cada respuesta:
- Idioma configurado vs. detectado
- Tiempo de procesamiento (en logs)
- Calidad del modelo usado
- Archivo de debug generado

## 🛣️ Roadmap

- [ ] Soporte para streaming de audio en tiempo real
- [ ] Métricas de rendimiento integradas
- [ ] Soporte para múltiples formatos de audio
- [ ] Cache de modelos para inicio más rápido
- [ ] API de administración para cambiar modelos dinámicamente

## 📄 Licencia

Este proyecto utiliza las siguientes tecnologías:
- **OpenAI Whisper**: MIT License
- **PyTorch**: BSD License
- **Flask**: BSD License

## 🆘 Soporte

Para reportar problemas o solicitar características:
1. Verificar la sección de troubleshooting
2. Revisar los logs del contenedor
3. Usar las funcionalidades de debug
4. Crear un issue con información detallada

---

**Desarrollado con ❤️ para el ecosistema puertocho-assistant** 