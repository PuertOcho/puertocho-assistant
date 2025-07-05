# PROJECT TRACKER – Puertocho Assistant Conversational Upgrade

> Última actualización: <!-- FECHA AUTO -->

Este documento desglosa las tareas necesarias para evolucionar la plataforma hacia un asistente conversacional multi-turno, capaz de ejecutar acciones en microservicios MCP (GitHub, Docker, Cursor, Taiga), consultar el tiempo y realizar búsquedas web.

---

## Leyenda de estados
- ✅ Completado
- 🔄 En progreso
- ⏳ Pendiente
- 🚧 Bloqueado

---

## Epic 0 – Preparación de Infraestructura
| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T0.1 | Agregar dependencias Redis al `pom.xml` de intentmanagerms. | – | ✅ |
| T0.2 | Configurar Redis en `intent-manager.yml` (host, port, TTL sesiones). | T0.1 | ✅ |
| T0.3 | Agregar nueva ruta `/api/assistant/**` en Gateway RouteConfig. | – | ✅ |
| T0.4 | Habilitar testing (quitar `maven.test.skip=true` del pom.xml). | – | ✅ |

## Epic 1 – Gestión de conversación y Slot Filling
| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T1.1 | Diseñar `ConversationState` (session Id → contexto) almacenado en Redis con TTL. | T0.2 | ✅ |
| T1.2 | Crear clase `DialogManager` que detecte entidades faltantes y genere preguntas de seguimiento. | T1.1 | ✅ |
| T1.3 | Refactorizar `SmartAssistantService` → delegue en `DialogManager` antes de ejecutar acciones. | T1.2 | ✅ |
| T1.4 | Tests unitarios y de integración multivuelta. | T1.3, T0.4 | ✅ |

## Epic 2 – API pública vía Gateway
| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T2.1 | Definir contrato REST `/assistant/chat` (JSON: prompt, sessionId). | – | ✅ |
| T2.2 | Añadir route en `gatewayms` y config en `configms`. | T2.1, T0.3 | ✅ |
| T2.3 | Agregar Swagger/OpenAPI docs centralizadas. | T2.2 | ✅ |

## Epic 3 – Cliente Raspberry Pi (wake-word-porcupine-version)
| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T3.1 | Sustituir llamada directa al servicio de transcripción por petición a `/assistant/chat`. | T2.1 | ✅ |
| T3.2 | Procesar respuesta: texto, parámetros opcionales, URL audio TTS. | T3.1 | ✅ |
| T3.3 | Integrar reproducción de audio (usar `mpv` o `pydub`). | T3.2 | ✅ |

## Epic 4 – Integración de herramientas MCP
| ID | Tool | Descripción | Estado |
|----|------|-------------|--------|
| T4.1 | GitHub | Crear `GithubTools` (list repos, crear issue, PR) consumiendo mcp-github-ms. | ⏳ |
| T4.2 | Docker | Crear `DockerTools` (listar images, start/stop container). | ⏳ |
| T4.3 | Cursor | Crear `CursorTools` (buscar fichero, editar línea). | ⏳ |
| T4.4 | Taiga | Reutilizar `taiga-mcp-ms` (crear story, listar tasks). | ⏳ |
| T4.5 | Weather | `WeatherService` llamando a Open-Meteo API. | ⏳ |
| T4.6 | Web Search | `WebSearchService` usando DuckDuckGo/Searx. | ⏳ |
| T4.7 | Actualizar entrenamiento NLU con nuevas intenciones y entidades. | T4.1-T4.6 | ⏳ |

## Epic 5 – NLU-MS evolución
| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T5.1 | Añadir intents y ejemplos YAML para MCP & utilidades. | T4.7 | ⏳ |
| T5.2 | Definir entidades: `repositorio`, `container`, `proyecto`, `ubicacion`. | T5.1 | ⏳ |
| T5.3 | Entrenar modelos (NLU + TFIDF, ensemble). | T5.2 | ⏳ |

## Epic 6 – Observabilidad y Seguridad
| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T6.1 | Centralizar logs en Elastic Stack; trazar conversación (sessionId). | – | ⏳ |
| T6.2 | Implementar rate-limit y auth tokens en Gateway. | T2.1 | ⏳ |
| T6.3 | Añadir tests e2e con Cypress (API) y Contract-Tests. | Todas | ⏳ |

---

## Roadmap sugerido
1. Epic 1 & 2 (semana 1-2)
2. Epic 3 paralelo a Epic 1 (semana 2-3)
3. Epic 4 & 5 (iterativo comenzando semana 3)
4. Epic 6 continuo

---

## Referencias
- Arquitectura orientada a microservicios de Puertocho Assistant (2025-07)
- Documentación MCP micro-servicios internos
- Rasa slot-filling & forms

--- 