# Resumen del Módulo `intentmanagerms`

Fecha de Resumen: 16 de julio de 2025

## 1. Propósito General

El microservicio `intentmanagerms` actúa como el cerebro orquestador del asistente Puertocho. Su responsabilidad principal es:
1.  Recibir la entrada del usuario a través de un endpoint.
2.  Clasificar la intención del usuario (NLU), ya sea mediante un servicio interno o delegando en `du-puertocho-ms` (Rasa).
3.  Gestionar el estado de la conversación y el flujo del diálogo.
4.  Orquestar la comunicación con otros microservicios (TTS, Taiga, etc.) para realizar acciones y obtener información.
5.  Devolver una respuesta final al usuario.

## 2. Estructura Detallada del Código Fuente

Ubicación: `src/main/java/com/intentmanagerms/`

### `IntentmanagermsApplication.java`
-   Punto de entrada de la aplicación Spring Boot.

### Paquete `application` (Lógica de Negocio)

#### `application/services`
Contiene la lógica de negocio principal. Clases más relevantes:
-   `DialogManager.java`: Gestiona el estado y flujo de la conversación. Parece ser central.
-   `SmartAssistantService.java`: Orquesta la interacción entre los diferentes servicios para formular una respuesta.
-   `NluService.java`: Se comunica con el servicio de NLU (posiblemente Rasa) para obtener la intención.
-   `DuService.java`: Específico para la comunicación con `du-puertocho-ms`.
-   `LlmIntentClassifierService.java`: Sugiere una clasificación de intenciones usando un LLM (posiblemente OpenAI).
-   `TtsService.java`: Interfaz para comunicarse con los servicios de Text-To-Speech.
-   `AuthService.java`: Maneja la lógica de autenticación (registro/login).
-   `AgentService.java`, `AssistantService.java`, `AuthenticatedChatService.java`: Parecen distintas implementaciones o facetas del asistente.

#### `application/tools`
Clases de utilidad que encapsulan la lógica para interactuar con herramientas o sistemas externos.
-   `TaigaTools.java`: Funciones para interactuar con la API de Taiga (crear tareas, obtener proyectos, etc.).
-   `SystemTools.java`: Herramientas relacionadas con el sistema (ej: obtener fecha y hora).
-   `SmartHomeTools.java`: Funciones para un futuro sistema de domótica.

#### `application/services/dto`
Data Transfer Objects (DTOs) para la comunicación entre capas y con el exterior.
-   `IntentInfo.java`, `IntentResponse.java`: Para manejar los datos de la intención.
-   `LoginRequest.java`, `RegisterRequest.java`: Para la autenticación.
-   `WebhookResponse.java`: Para responder a los webhooks de Rasa.

### Paquete `domain` (Core del Dominio)

#### `domain/model`
Representa las entidades centrales del dominio.
-   `ConversationState.java`: Modela el estado actual de una conversación.
-   `ConversationStatus.java`: Enum con los posibles estados de la conversación (e.g., `STARTED`, `AWAITING_RESPONSE`).

#### `domain/repository`
Interfaces para la persistencia de datos.
-   `ConversationRepository.java`: Define las operaciones para guardar y recuperar el estado de la conversación (probablemente en Redis).

### Paquete `infrastructure` (Detalles Técnicos)

#### `infrastructure/config`
Configuración de beans y componentes de infraestructura.
-   `RedisConfig.java`: Configura la conexión con Redis para la gestión de estado.
-   `RestTemplateConfig.java`: Configura el bean de `RestTemplate` para realizar llamadas HTTP a otros servicios.

#### `infrastructure/web`
Controladores REST que exponen la funcionalidad del microservicio.
-   `AssistantController.java`: Endpoint principal para interactuar con el asistente.
-   `NluController.java`: Endpoints para la funcionalidad de NLU.
-   `AgentController.java`: Otro endpoint de interacción, posiblemente para un "agente".
-   `AuthTestController.java`: Controlador para probar la autenticación.
-   `GlobalExceptionHandler.java`: Maneja las excepciones de forma centralizada.

## 3. Ficheros de Configuración y Despliegue

-   `pom.xml`: Define las dependencias del proyecto (Spring Boot, Redis, RestTemplate, etc.) y cómo construirlo.
-   `application.yml`: Contiene la configuración específica de la aplicación (puerto, conexión a Redis, URLs de otros servicios, etc.).
-   `dockerfile`: Instrucciones para empaquetar la aplicación en una imagen Docker para su despliegue.

## 4. Resumen de Tests

Ubicación: `src/test/java/com/intentmanagerms/`

-   Los tests existentes cubren partes de los servicios (`DialogManagerTest`, `SmartAssistantServiceTest`), herramientas (`TaigaToolsTest`) y controladores (`AssistantControllerTest`).
-   La cobertura de tests parece parcial y podría ser un área de mejora.
