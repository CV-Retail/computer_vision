# Kiosko de reconocimiento inteligente

Kiosko con cámara y pantalla que elige la publicidad según quién tiene enfrente, sin guardar ni identificar a nadie. La tienda crea sus propias campañas; el sistema detecta cuántas personas hay, su rango de edad, género, expresión facial y cuánto tiempo permanecen, y un motor de reglas decide qué mostrar (por ejemplo, pañales para un adulto con un bebé, y nunca alcohol si hay menores).

> **Estado:** fase de diseño. La estructura del proyecto existe, pero todavía no hay código de aplicación.

Diseño completo y decisiones: [docs/kiosko-arquitectura-mvp.md](docs/kiosko-arquitectura-mvp.md).

## Alcance del MVP

Una tienda, un PC Ubuntu dedicado (solo CPU) con pantalla y cámara, todo en local y sin nube. Incluye:

- Servicio de visión con edad (en rangos), género, expresión, número de personas y dwell time, procesado solo en memoria.
- Motor de reglas con vetos, audiencia, prioridad y playlist por defecto.
- Portal admin local con login: campañas, imágenes, plantilla simple.
- Importar y exportar campañas en JSON (con ZIP para los medios).
- Reportes agregados y página de estado local.

Fuera del MVP: nube, multi-tienda, licencias, monitoreo de flota, editor visual de plantillas, retail media, identificación de personas, Kubernetes.

## Arquitectura

```
Cámara → Servicio de visión → evento de audiencia (Redis) → Backend (motor de reglas) → Player → Pantalla
```

| Componente | Tecnología |
|---|---|
| Servicio de visión | Python 3.11, OpenCV, ONNX Runtime |
| Backend | Java 21, Spring Boot, Spring Modulith (Campañas, Reglas, Reportes) |
| Base de datos y bus | Postgres + Flyway, Redis pub/sub |
| Admin y player | React, Vite, TypeScript (rutas `/admin` y `/player`) |
| Entrenamiento | PyTorch, exportación a ONNX |
| Empaquetado | Docker Compose |

El servicio de visión no conoce al backend: solo publica eventos por un transporte configurable. Por eso el mismo contenedor corre en un único servidor o en estaciones separadas (PC o Raspberry Pi), con los perfiles de Compose `all-in-one` y `server` + `kiosk`. La cámara se elige por configuración: USB, CSI (Raspberry Pi) o RTSP.

Visión sin YOLO (AGPL): detección con PicoDet o NanoDet, rostro con YuNet o BlazeFace, tracking con ByteTrack y atributos con modelos pequeños entrenados con datos de licencia permisiva (por ejemplo FairFace).

## Estructura

```
services/vision-service/   visión (Python)
services/core-backend/     backend y reglas (Java)
apps/web/                  admin y player (React)
ml/                        entrenamiento y evaluación de modelos
contracts/                 esquemas JSON compartidos entre servicios
docs/                      arquitectura y decisiones (ADR)
```

Los esquemas de `contracts/` (`audience-event.v2` y `campaign.v2`) son la frontera entre Python y Java: ambos validan contra los mismos archivos.

## Reglas del motor

Orden fijo: horario y alcance → vetos → audiencia sobre el grupo → prioridad y peso → playlist por defecto. Un veto siempre gana. La decisión es determinista; la IA solo describe a las personas.

- Cada atributo lleva `confianza` y solo se usa si supera el umbral; si no, es "desconocido".
- En vetos, una edad desconocida cuenta como posible menor. Para alcohol se veta si alguien parece menor de 25.
- La expresión viene desactivada por defecto.
- Se evalúa un único grupo, el de la zona de interés de la cámara.

## Privacidad

Los frames se descartan, el seguimiento vive solo en memoria y se guardan únicamente agregados por ventana de tiempo, nunca una fila por persona. Antes del piloto hay que validar con un abogado la Ley 1581 de 2012 (datos biométricos) y la regulación de publicidad de alcohol, y revisar la licencia de cada modelo y dataset.

## Infraestructura local

Hoy `docker-compose.yml` solo levanta Postgres y Redis; el backend y la visión se añadirán cuando exista su código.

```bash
cp .env.example .env
docker compose --profile all-in-one up
```

Cambia los valores `change-me` de `.env` antes de arrancar.

## Flujo de trabajo (gitflow)

- Se trabaja en `feature/<TAREA>-<descripcion>` o `bugfix/<TAREA>-<descripcion>` y se abre un PR hacia `develop`.
- En cada PR y en cada push a `develop` corre el CI (`.github/workflows/ci.yml`): nombre de rama, contratos, Docker Compose y, cuando exista su código, visión, backend y web.
- `develop` se promueve a `main` con un PR manual, solo si el CI de `develop` está en verde. También se aceptan `release/*` y `hotfix/*`.
- Check obligatorio recomendado en la protección de ramas: `CI success`.

## Trabajo con agentes de IA

Las instrucciones para Claude Code y Gemini CLI viven en [AGENTS.md](AGENTS.md) (en inglés). `CLAUDE.md` lo importa y `.gemini/settings.json` apunta a él, así que ambas herramientas siguen las mismas reglas. Edita las reglas solo en `AGENTS.md`.

## Hoja de ruta

| Fase | Contenido |
|---|---|
| 0: MVP (semanas 1 a 4) | Una tienda, un PC, cinco atributos, reglas con vetos, portal admin, reportes |
| 1: robustecer | Varias estaciones, Raspberry Pi, editor de plantillas, entrenamiento con datos propios |
| 2: nube | Solo si hay varias tiendas pagando |
| 3: escala | Hardware del kiosko, updates remotos, despliegue masivo |

La semana 1 se dedica a licencias, precisión de los modelos y rendimiento en CPU.

## Decisiones abiertas

- Tipos de cámara del piloto (USB, CSI, RTSP).
- Quién fija los vetos mínimos obligatorios: la tienda, la empresa o un mínimo irrenunciable.
- Datos propios de entrenamiento: etiquetado, consentimiento y almacenamiento.
- Comportamiento con varios grupos frente al kiosko (la zona de interés es una propuesta a validar).
- Segmentos y anuncios iniciales.
