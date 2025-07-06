# Guía de Integración del Servicio NLU en Puerto Ocho

Esta guía documenta paso a paso cómo se añadió **Procesamiento de Lenguaje Natural (NLU)** basado en Rasa al ecosistema de microservicios de Puerto Ocho.

---

## 1. Estructura de Carpetas

```
puertocho-assistant/
├─ docs/
│  └─ integracion_nlu_puerto_ocho.md   ← ESTA GUÍA
├─ puertocho-assistant-server/
│  ├─ nlu-ms/                           ← Nuevo microservicio NLU
│  └─ intentmanagerms/                 ← Microservicio existente modificado
└─ scripts/
   └─ test_nlu_integration.sh          ← Script de pruebas end-to-end
```

---

## 2. Despliegue del Servicio NLU

1. **Clonar repo base**:
   ```bash
   git clone https://github.com/darshanpv/Rasa_NLU puertocho-assistant-server/nlu-ms/Rasa_NLU
   ```
2. **Reestructurar y copiar**:
   ```bash
   cd puertocho-assistant-server/nlu-ms
   mv Rasa_NLU/* . && rm -rf Rasa_NLU
   ```
3. **Dockerfile** optimizado:
   * Basado en `python:3.10-slim`.
   * Instala dependencias de `server/requirements.txt`.
   * Descarga modelo **spaCy español** y corpora NLTK (`stopwords`, `punkt`).
4. **Pipeline** (`server/core/config/es_spacy_diet.yml`):
   * `SpacyNLP` → `SpacyTokenizer` → `SpacyFeaturizer` → `RegexFeaturizer` → `LexicalSyntacticFeaturizer` → `CountVectors` → `DIETClassifier`.
5. **Datos de entrenamiento** (`server/training_data/intents/intents_es.yml`): 15 intenciones, 121 frases.
6. **docker-compose** (`puertocho-assistant-server/nlu-ms/docker-compose.yml`): expone **puerto 5001** y define health-check `GET /health`.

---

## 3. Entrenamiento del Modelo NLU

```bash
# Contenedor en ejecución primero
curl -X POST "http://localhost:5001/train?domain=intents&locale=es"
```
Devuelve:
```json
{"messageId":"TRAIN_SUCCESS","domain":"intents","locale":"es"}
```

---

## 4. Integración con `intentmanagerms`

### 4.1 DTOs (paquete `dto`)
* `NluResponse` → wrapper de la API.
* `NluMessage`, `NluIntent`, `NluEntity`, `NluIntentRanking`.

### 4.2 `NluService`
* **WebClient** (Spring WebFlux) consulta `/predict`, `/train`, `/health`.
* Manejo de time-outs y parsing JSON con Jackson.

### 4.3 `SmartAssistantService`
* Reemplaza la lógica basada en if-else por **intención → acción**.
* Umbral de confianza configurable (`nlu.confidence-threshold`).
* Ejemplo de flujo: `encender_luz` → `SmartHomeTools.encenderLuz(...)`.

### 4.4 Configuración (`intent-manager.yml`)
```yaml
nlu:
  url: ${NLU_URL:http://localhost:5001}
  domain: intents
  locale: es
  enabled: true
  confidence-threshold: 0.3
```

### 4.5 Dependencias
* Añadido `spring-boot-starter-webflux` al `pom.xml` para usar `WebClient`.

---

## 5. Endpoints REST

| Servicio | Método | Ruta | Descripción |
|----------|--------|------|-------------|
| **nlu-ms** | GET | `/health` | State check |
|  | POST | `/train?domain=&locale=` | Entrena modelo |
|  | POST | `/predict?domain=&locale=&userUtterance=` | Predice intención |
| **intentmanagerms** | GET | `/api/agent/health` | Health general |
|  | POST | `/api/agent/execute` | Ejecuta acción conversacional (internamente llama a NLU) |
|  | GET | `/api/nlu/health` | (Opcional) health NLU vía Intent Manager |
|  | POST | `/api/nlu/train` | Entrena modelo desde Intent Manager |

---

## 6. Pruebas End-to-End

Ejecutar el script:
```bash
./scripts/test_nlu_integration.sh
```
Salidas esperadas (resumen):
```
✅ NLU healthy
✅ Entrenamiento OK
🧪 "enciende la luz de la cocina" → encender_luz (cocina)
🧪 "apaga la luz del salón" → apagar_luz (salón)
🧪 "reproduce música" → reproducir_musica
✅ Intent Manager responde correctamente
```

---

## 7. Mantenimiento

1. **Añadir nuevas intenciones**: editar `intents_es.yml` (o crear nuevo dominio) y volver a entrenar.
2. **Cambiar umbral de confianza**: modificar `confidence-threshold` en Config Server.
3. **Actualizar dependencias**: `server/requirements.txt` en nlu-ms y `pom.xml` en intentmanagerms.

---

## 8. Referencias
* [Rasa Open Source 3.x – Tuning your model](https://legacy-docs-oss.rasa.com/docs/rasa/tuning-your-model/)
* [LangChain4j](https://github.com/langchain4j/langchain4j)
* [spaCy – Modelo `es_core_news_md`](https://spacy.io/models/es)

---

> Documentación generada automáticamente (jul 2025) por el asistente AI. 