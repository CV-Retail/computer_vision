# Kiosko de reconocimiento inteligente: arquitectura y MVP

*Revisión 2 · basada en el documento del 30 sep 2026 · @Rubén*

## Resumen

El MVP valida que un kiosko con cámara y pantalla puede elegir la publicidad según quién tiene enfrente, sin guardar ni identificar a nadie.

La pantalla muestra campañas que la propia tienda crea. El sistema detecta cuántas personas hay, su rango de edad, género, expresión facial y cuánto tiempo permanecen. Con eso decide: una madre con un bebé ve pañales, un grupo de jóvenes ve snacks. Un motor de reglas aplica restricciones, por ejemplo no mostrar alcohol si hay menores.

La referencia de mercado es DISPL, que combina analítica de audiencias anónima y señalización digital dirigida.

**Cambios de esta revisión:**

- Se elimina YOLO (AGPL-3.0). La visión usa solo componentes de licencia permisiva y modelos que podemos entrenar nosotros (ver "Pila de visión").
- Género y expresión participan en las reglas, siempre con umbral de confianza.
- El servicio de visión es una estación independiente desde el día uno: puede correr en el mismo servidor o en un equipo separado (PC Ubuntu hoy, Raspberry Pi a futuro).
- Se define cuándo se publica el evento de audiencia y qué pasa con varios grupos.
- Se asigna trabajo para un equipo de 2 o 3 personas.

## Decisiones de negocio

| Tema | Decisión |
|---|---|
| Cliente | La tienda (retail) |
| Modelo de venta | Licencia por kiosko |
| Quién crea el contenido y las campañas | La tienda. Nosotros damos asesoría y los medios para hacerlo |
| Qué se detecta | Dwell time, edad, género, expresión facial y número de personas |
| Uso de los atributos | Todos pueden usarse en reglas, con umbral de confianza. Expresión viene desactivada por defecto |
| Restricción de datos | No se guarda información de las personas ni se las identifica |
| Uso del grupo | Madre con bebé → pañales. Grupo de jóvenes → snacks, alcohol, etc. |
| Restricciones de contenido | El motor maneja vetos (por ejemplo, alcohol con menores presentes) |
| Movimiento de campañas entre tiendas | Importar y exportar en JSON |
| Escala objetivo | Muchas tiendas, pero no en el MVP |
| Nube y administración central | Fuera del MVP. Se construye cuando haya varias tiendas pagando |

## Alcance del MVP

El MVP es una sola tienda con un PC Ubuntu dedicado (solo CPU), pantalla y cámara, todo en local. No hay nube.

**Dentro del MVP:**

- Servicio de visión con los cinco atributos, procesado solo en memoria, con cámaras intercambiables por configuración.
- Motor de reglas con vetos, audiencia, prioridad y playlist por defecto.
- Portal admin local con login: crear campañas, subir imágenes, plantilla simple.
- Importar y exportar campañas en JSON, con un paquete ZIP para los medios.
- Reportes agregados por ventana de tiempo.
- Página de estado local para detectar fallas.
- Perfiles de despliegue `all-in-one` y `server` + `kiosk`.

**Fuera del MVP, de forma deliberada:**

- Nube, portal de empresa, multi-tienda, sincronización y licencias.
- Monitoreo de flota y alertas centralizadas.
- Varios kioskos a la vez en producción (se prueban en Fase 1, aunque la arquitectura ya lo permite).
- Editor visual de plantillas completo.
- Retail media y venta de espacios a marcas.
- Reconocimiento facial o identificación de personas.
- Integración con digital signage externo.
- Kubernetes (Docker Compose es suficiente).

## Arquitectura física

El servicio de visión no conoce al backend: solo publica eventos por un transporte configurable. Por eso el mismo contenedor sirve para un solo servidor o para estaciones separadas.

- **Perfil `all-in-one`:** un PC Ubuntu con cámara y pantalla corre backend, base de datos, visión y player.
- **Perfil `server` + `kiosk`:** el servidor corre backend, Postgres y Redis. Cada estación (PC o Raspberry Pi) corre visión y player, envía eventos de audiencia y recibe órdenes de reproducción por la red local.
- En desarrollo, el Mac corre el servicio de visión directamente (Docker Desktop no expone la webcam). En Ubuntu se pasa `/dev/video0` al contenedor.
- La imagen de visión se publica también para ARM64 (Raspberry Pi).

**Alternativa descartada por ahora:** dejar la visión en el servidor y enviar el video por la red. El kiosko sería más barato, pero el servidor crece con cada cámara y el video circula por la LAN.

### Perfil `all-in-one`

```
┌─ PC Ubuntu de la tienda ────────────────────────────────────────────┐
│  Postgres y Redis  ◄──  Servicio de visión  ◄──────  Cámara        │
│  (datos y eventos)      (Python, ONNX Runtime)                      │
│        ▲                                                            │
│        ▼                                                            │
│  Backend           ──►  Ad Player          ──────►  Pantalla       │
│  (Spring Boot,          (Chromium modo kiosko)                     │
│   Modulith)                                                         │
└────────────▲────────────────────────────────────────────────────────┘
             │
        Admin web (laptop o tablet)
```

### Perfil `server` + `kiosk`

```
┌─ Servidor de la tienda ─┐            ┌─ Estación 1: PC o Raspberry Pi ─────────┐
│ Backend                 │  Eventos ↑ │ Visión (Python) │ Player (Chromium)      │
│ (Spring Boot, Modulith) │◄──────────►└─────────────────────────────────────────┘
│          ▲              │  Órdenes ↓
│          ▼              │            ┌─ Estación 2 ────────────────────────────┐
│ Postgres y Redis        │  Eventos ↑ │ Visión (Python) │ Player (Chromium)      │
└─────────────────────────┘◄──────────►└─────────────────────────────────────────┘
                             Órdenes ↓
```

### Fuentes de cámara

`ICameraSource` es una interfaz interna del servicio de visión. La fuente se elige por configuración:

| Fuente | Uso |
|---|---|
| `UsbCameraSource` (V4L2/OpenCV) | Webcams USB |
| `CsiCameraSource` (picamera2) | Cámara de Raspberry Pi |
| `RtspCameraSource` | Cámaras IP |

## Flujo funcional paso a paso

1. La tienda crea la campaña "Pañales" en el portal admin: contenido, audiencia, horario y vetos.
2. Una persona se acerca. La cámara alimenta al servicio de visión y el kiosko sigue con la playlist por defecto.
3. El servicio detecta personas y rostros, y para cada una estima edad en rangos, género y expresión, cada uno con su confianza. Cuenta el dwell time con un seguimiento temporal. Los frames se descartan.
4. Se resume un grupo anónimo: composición, atributos y tiempo.
5. El servicio publica el evento de audiencia versionado (ver "Cuándo se publica").
6. El backend lo traduce a un evento de dominio y el motor decide: horario, vetos, audiencia y prioridad.
7. El backend envía la orden por WebSocket al player, que cambia de playlist. Cada campaña tiene un tiempo mínimo en pantalla.
8. Cuando el grupo sale de cámara se cierra el dwell time, se suma al contador de la ventana y se borra de memoria todo lo individual.
9. El backend guarda el conteo agregado y las reproducciones; la tienda los ve en los reportes.

### Cuándo se publica el evento

- Al cambiar la composición del grupo (entra o sale alguien, cambia un rango de edad estable).
- Cada N segundos (propuesta: 2) mientras el grupo siga presente, como latido.
- Una vez más al cerrar el dwell, con `dwellMs` final.

La composición cambia solo si el nuevo valor se mantiene unos frames, para no reaccionar a errores puntuales.

### Varios grupos frente al kiosko

Propuesta inicial, a validar con pruebas reales: el servicio resume **un único grupo**, formado por las personas dentro de la zona de interés de la cámara (rectángulo configurable). Si hay personas lejanas o de paso, no cuentan. El motor siempre evalúa un grupo.

## Pila de visión (sin YOLO)

Todo se entrena con PyTorch (BSD) y se exporta a ONNX para inferir con ONNX Runtime (MIT) en CPU, incluido ARM.

| Etapa | Opción recomendada | Licencia | Nota |
|---|---|---|---|
| Detección de personas | PP-PicoDet o NanoDet; alternativa YOLOX-Nano | Apache-2.0 | Ligeros, entrenables y viables en Raspberry Pi |
| Detección de rostro | YuNet (OpenCV Zoo) o BlazeFace (MediaPipe) | MIT / Apache-2.0 | Muy rápidos en CPU. Evitar los pesos preentrenados de SCRFD/InsightFace (no comercial) |
| Tracking y dwell | ByteTrack o Norfair | MIT / BSD | Solo en memoria |
| Edad y género | MobileNetV3 o EfficientNet-Lite (timm, Apache-2.0) entrenado con FairFace | Dataset CC BY 4.0 | UTKFace, IMDB-WIKI y similares son solo para investigación |
| Expresión | Misma arquitectura pequeña | Por revisar | Los datasets buenos (AffectNet, RAF-DB) son no comerciales. Es el punto más débil |

Las licencias de cada peso y dataset se verifican antes del piloto, no se asumen.

### Entrenamiento y evaluación

- Dataset base de edad y género: FairFace, más datos propios etiquetados en la tienda piloto con consentimiento.
- Edad en rangos (bebé, niño, joven, adulto, mayor). Bebés y niños se evalúan aparte porque los modelos fallan más ahí.
- Cada predicción lleva `confianza`. El umbral se calibra con un set de prueba propio.
- Metas a fijar en la semana 1: precisión mínima por rango y fps mínimos en CPU.

## Stack tecnológico por componente

| Componente | Tecnología | Por qué |
|---|---|---|
| Servicio de visión | Python 3.11, OpenCV, ONNX Runtime | Captura, detección y tracking en un solo servicio, sin AGPL |
| Entrenamiento | PyTorch, timm | Modelos propios, exportables a ONNX |
| Backend | Java 21, Spring Boot, Spring Modulith | Dominio: campañas, reglas, vetos |
| Base de datos | Postgres con Flyway | Campañas, reglas y agregados desde el día uno |
| Bus de eventos | Redis (pub/sub), con contraseña cuando sale de localhost | Frontera entre visión y backend. Transporte configurable |
| Admin de la tienda | React, Vite, TypeScript | SPA servida por el backend |
| Ad Player | La misma app React, ruta `/player`, en Chromium modo kiosko | Un solo proyecto frontend con dos rutas |
| Archivos de medios | Volumen en disco, ruta en Postgres | Suficiente para el MVP |
| Empaquetado | Docker Compose con perfiles `all-in-one` y `server`/`kiosk` | Un comando levanta cada perfil |

El portal admin lleva login local simple (Spring Security con un usuario administrador).

## Motor de reglas

Orden fijo de evaluación; un veto siempre gana sobre cualquier coincidencia de audiencia.

1. Filtrar campañas por horario y alcance (tienda o kiosko).
2. Aplicar vetos. Ejemplo: no mostrar alcohol si hay algún menor estimado.
3. Evaluar la audiencia sobre la composición del grupo.
4. Si varias campañas coinciden, desempatar por prioridad y peso.
5. Si ninguna coincide, mostrar la playlist por defecto.

**Condiciones de audiencia:** mínimo y máximo de personas por rango de edad, género opcional y expresión opcional, evaluadas sobre el grupo.

**Criterios de diseño:**

- Cada atributo se usa solo si su confianza supera el umbral. Si no, es "desconocido" y no activa campañas.
- **Vetos con política opuesta:** un atributo desconocido en edad cuenta como posible menor. Se prefiere no mostrar el anuncio a mostrarlo por error.
- Veto del alcohol conservador: no mostrar si alguien parece menor de 25.
- Una campaña se mantiene unos segundos mínimos antes de cambiar.
- La decisión es determinista y no usa IA. La IA solo describe a las personas.
- El género estimado es binario y la expresión es poco fiable. Se miden con pruebas reales antes de prometerlos a un cliente.

## Contratos entre componentes

Dos esquemas JSON versionados, independientes del lenguaje, en `contracts/`.

### Evento de audiencia

```json
{
  "schemaVersion": 2,
  "kioskId": "k1",
  "timestamp": "2026-08-26T18:00:03Z",
  "motivo": "cambio_composicion",
  "personas": [
    {
      "edad": { "valor": "adulto_25_34", "confianza": 0.82 },
      "genero": { "valor": "F", "confianza": 0.91 },
      "expresion": { "valor": "feliz", "confianza": 0.55 }
    },
    { "edad": { "valor": "bebe", "confianza": 0.74 } }
  ],
  "dwellMs": 3000
}
```

`motivo` es `cambio_composicion`, `latido` o `cierre_dwell`. El evento no lleva cajas ni ids de tracking.

### Campaña

Ilustrativo; el esquema final se define en la primera semana:

```json
{
  "schemaVersion": 2,
  "storeId": "tienda-001",
  "campaign": {
    "id": "c-panales",
    "name": "Pañales",
    "status": "draft",
    "priority": 50,
    "schedule": { "from": "08:00", "to": "20:00" },
    "audience": {
      "edad": { "adulto": { "min": 1 }, "bebe": { "min": 1 } },
      "genero": null,
      "expresion": null,
      "minConfianza": 0.6
    },
    "vetoes": ["alcohol_con_menores"],
    "playlist": [ { "mediaRef": "sha256:...", "durationSec": 10 } ]
  }
}
```

**Reglas para la importación:**

- El JSON referencia los archivos por id o hash; para mover contenido entre tiendas se exporta un ZIP con `manifest.json` y los medios.
- Al importar se valida contra el esquema y la campaña se crea como borrador, nunca activa. El archivo se trata como entrada no confiable.
- `storeId` y `kioskId` van en todos los datos desde el inicio.

## Privacidad y cumplimiento

El sistema procesa imágenes solo en memoria y guarda únicamente conteos agregados anónimos.

- Los frames se procesan y se descartan. No se guardan fotos, video ni embeddings faciales.
- El seguimiento temporal vive solo en memoria mientras la persona está en cámara.
- Se guardan agregados por ventana de tiempo y campaña, nunca una fila por persona.
- Si una ventana tiene muy pocas personas, el dato puede señalar a un individuo; se aplica un mínimo de agrupación.
- Aviso visible de cámara en cada kiosko y política de tratamiento de datos.
- Si se recogen imágenes para entrenar, se hace con consentimiento y fuera del flujo de producción.

**Temas para validar antes del piloto:**

- **Ley 1581 de 2012 (Colombia):** la imagen facial puede tratarse como dato biométrico sensible. Estimar edad o género sin identificar ni guardar requiere revisión de un abogado.
- **Regulación de publicidad de alcohol.**
- **Reconocimiento de emociones:** precisión dudosa, regulación más estricta en otras jurisdicciones y datasets sin licencia comercial clara. Queda desactivado por defecto.
- **Licencias:** revisar peso por peso y dataset por dataset. Se evita YOLO/Ultralytics y SCRFD/InsightFace preentrenado.

## Monitoreo y fallas

Sin nube, el MVP detecta las fallas con una página de estado local en el portal admin (solo se construye en el MVP; Fase 1 la amplía para varias estaciones).

| Falla | Cómo se detecta en el MVP |
|---|---|
| Cámara desconectada o sin frames | El servicio de visión reporta que no recibe frames |
| Player atascado | El player envía un pulso periódico al backend |
| Estación apagada o sin red | Ausencia del pulso del player |
| Inferencia lenta | Métrica de fps del servicio de visión |
| Disco lleno o servicio caído | Spring Actuator y un endpoint de salud en visión |
| Servidor caído o sin internet | No se detecta en el MVP. Requiere un observador externo (nube, Fase 2) |

## Reportes

Guardan agregados, no personas:

- Conteos por ventana (por ejemplo 5 minutos), rango de edad, género y expresión (cuando esté activa).
- Dwell time medio por ventana.
- Reproducciones por campaña (cuántas veces y cuántos segundos se mostró), para que la tienda vea qué campaña se activó con qué audiencia.

## Repositorios y estructura

Un solo repositorio (monorepo):

```
kiosko/
├── services/
│   ├── vision-service/   # Python · cámara + detección + atributos + tracking (ONNX)
│   └── core-backend/     # Java 21 · Spring Boot + Modulith
├── apps/
│   └── web/              # React + Vite · rutas /admin y /player
├── ml/                   # entrenamiento y evaluación de modelos (PyTorch)
├── contracts/            # audience-event y campaign, versionados
├── docs/                 # arquitectura y decisiones (ADR)
├── docker-compose.yml    # perfiles all-in-one, server y kiosk
└── README.md
```

- `contracts/` es la frontera: Python y Java validan contra los mismos esquemas.
- `ml/` evita mezclar entrenamiento con el servicio de producción; solo exporta modelos `.onnx` versionados.
- Dentro del backend, tres módulos de Spring Modulith: Campañas, Reglas y Reportes.

**Cuándo crear repositorios adicionales (no en el MVP):** la nube (Fase 2), equipos de visión y backend con ritmos distintos, o imágenes de sistema para los kioskos.

## Reparto del equipo (2 o 3 personas)

| Rol | Responsabilidad |
|---|---|
| Visión y ML | Pila de visión, entrenamiento, benchmark en CPU y ARM, `ICameraSource` |
| Backend y reglas | Motor de reglas, campañas, reportes, contratos, perfiles Docker |
| Frontend y player | Portal admin, player, importar/exportar, página de estado |

Con dos personas, backend y frontend se combinan, y el player se mantiene lo más simple posible.

## Hoja de ruta por fases

| Fase | Momento | Contenido |
|---|---|---|
| **Fase 0: MVP** | Semanas 1 a 4 | Una tienda, un PC Ubuntu · Cinco atributos · Reglas con vetos · Portal admin local · JSON de campañas · Reportes · Página de estado |
| **Fase 1: robustecer** | Después del MVP | Varias estaciones, Raspberry Pi · Editor de plantillas · Más casos de reglas · Entrenamiento con datos propios |
| *Compuerta* | | *Solo si hay varias tiendas pagando* |
| **Fase 2: nube** | Tras la compuerta | Heartbeat y alertas · Inventario de tiendas · Distribuir campañas |
| **Fase 3: escala** | Más adelante | Hardware del kiosko · Updates remotos · Despliegue masivo |

La semana 1 de la Fase 0 se dedica a licencias, precisión de los modelos y rendimiento en CPU.

## Riesgos y decisiones abiertas

| Tema | Por qué importa | Cuándo decidir |
|---|---|---|
| Licencias de pesos y datasets | Muchos solo permiten investigación. Expresión es el caso más débil | Semana 1 |
| Precisión con niños y bebés | Base de los vetos y de campañas como pañales | Semana 1, con pruebas reales |
| Rendimiento en CPU y Raspberry Pi | Define modelos y fps alcanzables | Semana 1, con benchmark |
| Cumplimiento legal | Ley 1581 y publicidad de alcohol | Antes del piloto |
| Tipos de cámara del piloto | USB, CSI o RTSP | Semana 1 |
| Varios grupos frente al kiosko | La propuesta de zona de interés debe validarse | Semana 2 |
| Canal de eventos con estaciones remotas | Redis con contraseña en LAN o HTTP/MQTT | Fase 1 |
| Vetos mínimos obligatorios | Quién los define: la tienda, nuestra empresa o un mínimo irrenunciable | Antes del piloto |
| Datos propios de entrenamiento | Etiquetado, consentimiento y almacenamiento | Semana 2 |
| Segmentos y anuncios iniciales | Desbloquea el resto del trabajo | Primera semana |

## Próximos pasos

1. Definir 2 o 3 segmentos de audiencia y qué anuncio corresponde a cada uno.
2. Verificar licencias de detector, rostro, pesos y datasets; benchmark de CPU en el PC Ubuntu y con límite equivalente a Raspberry Pi 5.
3. Crear el monorepo con la estructura, `docker-compose.yml` con perfiles y los esquemas JSON iniciales.
4. Implementar `ICameraSource` con USB y, después, CSI y RTSP.
5. Publicar un evento simple en Redis y consumirlo en el backend, de punta a punta.
6. Construir el motor de reglas con vetos y el player en Chromium modo kiosko.
7. Construir el portal admin con login, subida de imágenes, importar/exportar JSON, reportes y página de estado.
