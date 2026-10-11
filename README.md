# Kiosko de reconocimiento inteligente

Kiosko con cámara y pantalla que elige la publicidad según quién tiene enfrente, sin guardar ni identificar a nadie. La tienda crea sus propias campañas; el sistema detecta cuántas personas hay, su rango de edad, género, expresión facial y cuánto tiempo permanecen, y un motor de reglas decide qué mostrar (por ejemplo, pañales para un adulto con un bebé, y nunca alcohol si hay menores).

Diseño completo y decisiones: [docs/kiosko-arquitectura-mvp.md](docs/kiosko-arquitectura-mvp.md). Conceptos y vistas del portal de administración: [docs/Administración de campañas conceptos y vistas del administrador.md](docs/Administración%20de%20campañas%20conceptos%20y%20vistas%20del%20administrador.md).

## Estado

Los cuatro esqueletos existen, se construyen, pasan sus pruebas y arrancan juntos con Docker Compose. Todavía **no hay lógica de negocio**: el reconocimiento, el motor de reglas, las campañas y el portal están por construir.

| Pieza | Estado |
|---|---|
| CI con GitHub Actions y gitflow (KIO-1) | Hecho |
| Esqueleto de visión, Python (KIO-6) | Hecho: paquete, pruebas e imagen; no captura ni detecta todavía |
| Esqueleto del backend, Java (KIO-7) | Hecho: módulos Campañas, Reglas y Reportes con capas validadas por pruebas, migraciones, `/actuator/health` |
| Esqueleto de la web, React (KIO-8) | Hecho: rutas `/admin` y `/player` con vistas de ejemplo |
| Docker Compose completo (KIO-9) | Hecho: perfiles `all-in-one`, `server` y `kiosk`, proxy único y bus de eventos (Valkey, protocolo Redis) con contraseña |
| Documento de administración de campañas (KIO-47) | Hecho |
| Cámara, detección, atributos, eventos, reglas, campañas, medios, login, reportes, estado | Planeado |

## Cómo levantarlo

Requisitos: Docker con Compose. Copia la configuración y cambia las contraseñas `change-me`:

```bash
cp .env.example .env
```

**Todo en un equipo** (servidor y estación juntos):

```bash
docker compose --profile all-in-one up --build -d
```

**Servidor y estaciones separadas:**

```bash
# En el servidor: pon REDIS_BIND_ADDR en .env a la IP de la LAN y arranca
docker compose --profile server up --build -d

# En cada estación: .env con EVENT_TRANSPORT_URL=redis://:<contraseña>@<servidor>:6379/0
docker compose --profile kiosk up --build
```

Para detener y borrar todo: `docker compose --profile all-in-one down -v` (usa el mismo perfil con el que arrancaste).

**Puntos de entrada.** Solo se publica el servicio web (nginx), en el puerto `WEB_PORT` (por defecto 80):

- Administración: `http://<servidor>/admin`
- Player de la pantalla: `http://<servidor>/player` (Chromium en modo kiosko; hoy se abre a mano)
- El backend es interno: nginx envía `/api` y `/ws` al backend. `/actuator` no se expone.

**Variables** (`.env.example`):

| Variable | Para qué sirve |
|---|---|
| `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` | Base de datos |
| `REDIS_PASSWORD` | Obligatoria: el bus de eventos no arranca sin ella |
| `WEB_PORT` | Puerto del host para la web (cámbialo si el 80 está ocupado) |
| `REDIS_BIND_ADDR` | Dirección en la que escucha el bus de eventos (`127.0.0.1` = solo esa máquina) |
| `EVENT_TRANSPORT_URL` | Solo en una estación: dirección del bus de eventos del servidor |

Dos advertencias:

- El bus de eventos queda publicado en el servidor (puerto 6379) para que las estaciones envíen eventos. Mantén ese puerto en la red local de la tienda y nunca lo expongas a internet.
- No hay HTTPS en el MVP: el login del administrador viajaría sin cifrar por la red local de la tienda. Es una limitación aceptada y documentada, a revisar antes del piloto.

El servicio y las variables se llaman `redis` y `REDIS_*`, y las URL son `redis://...`, porque el protocolo es el de Redis. La imagen es **Valkey** (fork BSD-3-Clause de Redis 7.2) y no Redis, porque Redis 7.4 y posteriores ya no usan licencia BSD (RSALv2/SSPLv1).

En una estación, Compose imprime avisos de variables de PostgreSQL sin definir: son esperados e inofensivos.

## Alcance del MVP

Una tienda, un PC Ubuntu dedicado (solo CPU) con pantalla y cámara, todo en local y sin nube. Incluye:

- Servicio de visión con edad (en rangos), género, expresión, número de personas y dwell time, procesado solo en memoria.
- Motor de reglas con vetos, audiencia, prioridad y playlist por defecto.
- Portal admin local con login: campañas, reglas de audiencia, vetos, medios, reportes y estado.
- Importar y exportar campañas en JSON (con ZIP para los medios).

Fuera del MVP: nube, multi-tienda, licencias, monitoreo de flota, editor visual de plantillas, retail media, identificación de personas, Kubernetes.

## Arquitectura

```
Cámara → Visión → evento de audiencia (bus Redis/Valkey) → Backend (motor de reglas) → orden por WebSocket → Player → Pantalla
Navegador / Chromium → web (nginx :80) → /admin, /player ... aplicación;  /api, /ws → backend
```

| Componente | Tecnología |
|---|---|
| Servicio de visión | Python 3.11, ONNX Runtime |
| Backend | Java 21, Spring Boot 4.1, Spring Modulith 2.1, Maven Wrapper |
| Base de datos y bus | PostgreSQL con Flyway (migraciones por motor), bus de eventos pub/sub con Valkey (compatible con el protocolo de Redis, licencia BSD-3) y contraseña |
| Admin y player | React 18, Vite, TypeScript; servidos por nginx (que además hace de proxy hacia el backend) |
| Entrenamiento | PyTorch, exportación a ONNX (planeado) |
| Empaquetado | Docker Compose con perfiles `all-in-one`, `server` y `kiosk` |

- El servicio de visión no conoce al backend: solo publica eventos por un transporte configurable. Por eso el mismo contenedor corre en un único servidor o en estaciones separadas (PC o Raspberry Pi).
- El backend sigue arquitectura limpia con DDD: cada módulo tiene `domain`, `application` e `infrastructure`, y pruebas automáticas rompen el build si el dominio depende de un framework. El dominio no conoce el motor de base de datos; cambiar de motor es una carpeta de migraciones, un driver y configuración. Detalle en [services/core-backend/README.md](services/core-backend/README.md).
- Visión sin YOLO (AGPL): detección con PicoDet o NanoDet, rostro con YuNet o BlazeFace, tracking con ByteTrack y atributos con modelos pequeños entrenados con datos de licencia permisiva (por ejemplo FairFace).

## Estructura

```
services/vision-service/   visión (Python)
services/core-backend/     backend y reglas (Java)
apps/web/                  admin y player (React) con su nginx
ml/                        entrenamiento y evaluación de modelos
contracts/                 esquemas JSON compartidos entre servicios
spec/                      requisitos, diseño y tareas de cada feature
docs/                      arquitectura y documentos de producto
.github/                   CI, plantilla de PR y automatización de PR
AGENTS.md                  reglas para Claude Code y Gemini CLI
```

Los esquemas de `contracts/` (`audience-event.v2` y `campaign.v2`) son la frontera entre Python y Java: ambos validan contra los mismos archivos.

## Reglas del motor

Orden fijo: horario y alcance → vetos → audiencia sobre el grupo → prioridad y peso → playlist por defecto. Un veto siempre gana. La decisión es determinista; la IA solo describe a las personas.

- Cada atributo lleva `confianza` y solo se usa si supera el umbral; si no, es "desconocido".
- En vetos, una edad desconocida cuenta como posible menor. Para alcohol se veta si alguien parece menor de 25.
- La expresión viene desactivada por defecto.
- Se evalúa un único grupo, el de la zona de interés de la cámara.

## Privacidad

Los frames se descartan, el seguimiento vive solo en memoria y se guardan únicamente agregados por ventana de tiempo, nunca una fila por persona. Antes del piloto hay que validar con un abogado la Ley 1581 de 2012 (datos biométricos) y la regulación de publicidad de alcohol, y revisar la licencia de cada modelo, dataset e imagen de contenedor.

## Flujo de trabajo

**Gitflow.** Se trabaja en `feature/<TAREA>-<descripcion>` o `bugfix/<TAREA>-<descripcion>` (por ejemplo `feature/KIO-9-compose-complete`) y se abre un PR hacia `develop`. `develop` se promueve a `main` con un PR manual, solo si el CI está en verde; también se aceptan `release/*` y `hotfix/*`.

**Una tarea por tarjeta de Trello** (`KIO-N`) y un spec por feature en `spec/<TAREA>-<descripcion>/`: primero `requirements.md`, luego `design.md` y `tasks.md`, cada uno aprobado antes del siguiente, y solo entonces el código.

**CI** (`.github/workflows/ci.yml`), en cada PR y en cada push a `develop` y `main`: nombre de rama, contratos, Docker Compose (perfiles, contenido de cada perfil y configuración de nginx), una prueba de humo que levanta el perfil `server`, y visión, backend y web. El único check obligatorio es `CI success`. Al abrir un PR, `pr-automation.yml` pide revisión a los demás miembros y pone etiquetas según la rama.

**Agentes de IA.** Las reglas para Claude Code y Gemini CLI viven en [AGENTS.md](AGENTS.md) (en inglés). `CLAUDE.md` lo importa y `.gemini/settings.json` apunta a él. Edita las reglas solo en `AGENTS.md`.

## Hoja de ruta

| Fase | Contenido |
|---|---|
| 0: MVP (semanas 1 a 4) | Una tienda, un PC, cinco atributos, reglas con vetos, portal admin, reportes |
| 1: robustecer | Varias estaciones, Raspberry Pi, editor de plantillas, entrenamiento con datos propios |
| 2: nube | Solo si hay varias tiendas pagando |
| 3: escala | Hardware del kiosko, updates remotos, despliegue masivo |

## Decisiones abiertas

- Quién fija los vetos mínimos obligatorios: la tienda, la empresa o un mínimo irrenunciable.
- Cumplimiento legal antes del piloto: Ley 1581 y publicidad de alcohol.
- HTTPS en la red local: hoy es una limitación aceptada.
- Tipos de cámara del piloto (USB, CSI, RTSP).
- Datos propios de entrenamiento: etiquetado, consentimiento y almacenamiento.
- Comportamiento con varios grupos frente al kiosko (la zona de interés es una propuesta a validar).
- Del documento de administración: prioridad numérica o alta/media/baja, dwell time como condición de regla, formatos y peso de los medios, y política de contraseñas.
