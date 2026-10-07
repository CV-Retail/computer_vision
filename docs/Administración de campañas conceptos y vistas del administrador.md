# Administración de campañas: conceptos y vistas del administrador

Oct 7, 2026 · @Rubén

## Resumen

El administrador de la tienda decide qué se muestra en el kiosko combinando cinco piezas: medios, playlists, reglas de audiencia, vetos y campañas.

Una campaña junta contenido (los fliers), a quién va dirigida (una regla de audiencia), cuándo corre y qué la bloquea (los vetos). Este documento define cada pieza y sus atributos, y luego muestra, en wireframes sin colores, las vistas que el administrador usa para crearlas y para vigilar el kiosko.

Es el complemento de negocio del documento de arquitectura. Todo lo que aquí se propone se apoya en el motor de reglas ya definido: horario, vetos, audiencia, prioridad y playlist por defecto, en ese orden.

## Conceptos y atributos

Son cinco conceptos, y la campaña es el único que el kiosko ejecuta; los otros cuatro son las piezas con las que se arma.

### Medio (flyer)

Es un archivo de imagen o video que la tienda sube y que luego se usa en una o varias playlists.

| Atributo | Qué es | Ejemplo |
| --- | --- | --- |
| Nombre | Cómo lo identifica el administrador | Flyer cerveza viernes |
| Archivo y tipo | Imagen (por ejemplo JPG o PNG) o video (por ejemplo MP4) | flyer-cerveza.jpg |
| Orientación y resolución | Se leen al subir el archivo. El sistema avisa si no encaja con la pantalla del kiosko | Horizontal, 1920×1080 |
| Duración sugerida | Segundos en pantalla para una imagen. En un video, su duración real | 10 s |
| Etiquetas de contenido | Categorías que usan los vetos para decidir qué bloquear | alcohol, snacks, higiene |
| Estado | En uso (está en alguna playlist) o sin usar | En uso en 2 campañas |
| Fecha de subida | Cuándo se cargó | 2026-10-07 |

### Playlist

Es la lista ordenada de medios que muestra una campaña, con el tiempo de cada pieza. No se administra por separado: vive dentro de la campaña.

| Atributo | Qué es | Ejemplo |
| --- | --- | --- |
| Medios | Los fliers que la componen | 3 piezas de pañales |
| Orden | Secuencia en que se muestran, en bucle | 1, 2, 3 |
| Duración por pieza | Segundos que dura cada medio en pantalla | 10 s, 10 s, 15 s |

La playlist por defecto es una campaña especial sin audiencia: se muestra cuando ninguna otra coincide, o cuando falla la cámara.

### Regla de audiencia

Describe a qué grupo de personas va dirigida una campaña. Es reutilizable: se crea una vez ("Madre con bebé") y varias campañas pueden usarla.

| Atributo | Qué es | Ejemplo |
| --- | --- | --- |
| Nombre y descripción | Para reconocerla en la lista | Madre con bebé |
| Personas en el grupo | Mínimo y máximo de personas | De 2 a 3 |
| Edad por rango | Mínimo y máximo de personas en cada rango: bebé, niño, joven, adulto, mayor | Adulto ≥ 1, bebé ≥ 1 |
| Género (opcional) | Mínimo de personas de un género | Al menos 1 mujer |
| Expresión (opcional) | Viene desactivada por defecto por su baja fiabilidad | Desactivada |
| Confianza mínima | Cuán seguro debe estar el sistema de un atributo para usarlo. Si no llega, el atributo cuenta como desconocido y no activa la campaña | 0,6 |

### Veto

Es una restricción que impide mostrar cierto contenido cuando el grupo cumple una condición. Un veto siempre gana sobre cualquier coincidencia de audiencia.

| Atributo | Qué es | Ejemplo |
| --- | --- | --- |
| Nombre | Para reconocerlo | Alcohol con menores |
| Condición | Qué debe ocurrir en el grupo para que el veto se active | Alguien parece menor de 25 años |
| Edad desconocida | Qué hacer si la edad no es fiable. Para vetos, el valor seguro es contarla como posible menor | Cuenta como menor |
| Qué bloquea | Las etiquetas de contenido que no se pueden mostrar | alcohol |
| Alcance | Dónde aplica: toda la tienda o un kiosko | Toda la tienda |
| Origen | Del sistema (mínimo obligatorio, no se puede quitar) o creado por la tienda | Del sistema |
| Estado | Activo o inactivo. Los del sistema siempre están activos | Activo |

### Campaña

Es la unidad que se programa y se muestra. Junta una playlist con una regla de audiencia, los vetos que le aplican y un calendario.

| Atributo | Qué es | Ejemplo |
| --- | --- | --- |
| Nombre | Cómo la ve el administrador | Pañales tarde |
| Estado | Borrador, programada, activa, pausada o finalizada | Activa |
| Prioridad | Desempata cuando varias campañas coinciden: alta, media o baja | Media |
| Regla de audiencia | La regla a la que va dirigida (una por campaña) | Madre con bebé |
| Playlist | Los medios y sus duraciones | 3 piezas de pañales |
| Etiquetas de contenido | Se calculan desde los medios de la playlist. Es lo que los vetos revisan | higiene |
| Vetos que aplican | Los que se activan por sus etiquetas, más los que el administrador agregue a mano | Ninguno |
| Programación | Fechas de inicio y fin, días de la semana y franja horaria | Lun a vie, 8:00 a 20:00 |
| Alcance | En qué kioskos corre | Todos |
| Tiempo mínimo en pantalla | Segundos antes de que pueda cambiar a otra campaña, para que la pantalla no parpadee | 8 s |

## Cómo se relacionan

La campaña es el centro: apunta a una regla de audiencia, contiene una playlist de medios y recibe los vetos que le aplican.

&#91;embedded content: relación entre campaña, regla, playlist, vetos y medios\]

Los vetos no miran la playlist directamente: miran las etiquetas de los medios que la componen.

## Recorrido del administrador

Para publicar una campaña, el administrador sigue siempre el mismo orden: primero los insumos (medios, reglas, vetos) y al final la campaña que los junta.

1. Sube los fliers a la biblioteca de medios y les pone etiquetas de contenido (por ejemplo, alcohol).
2. Revisa los vetos activos de su tienda y crea los propios si los necesita.
3. Crea la regla de audiencia, o elige una existente.
4. Crea la campaña: elige la regla, arma la playlist, define la programación y el alcance.
5. Revisa el resumen de la campaña, que muestra qué vetos le aplican y avisa de problemas.
6. Guarda como borrador, o la programa o activa.
7. Sigue los resultados en reportes.

Avisos que el sistema debe mostrar antes de activar:

- La campaña no tiene medios o no tiene regla de audiencia.
- Un medio no coincide con la orientación o resolución de la pantalla.
- La campaña lleva la etiqueta alcohol y no hay un veto activo que la cubra.
- Hay otra campaña con la misma regla, horario y prioridad, y el desempate será impredecible.

## Vistas del administrador

El administrador usa siete vistas dentro del portal admin local: una de inicio de sesión y seis con el mismo menú superior: Campañas, Reglas, Vetos, Medios, Reportes y Estado.

Reportes ya está definido en el documento de arquitectura y todavía no se maqueta. Los wireframes muestran solo estructura, sin colores ni estilo final. Un rectángulo con una X es una imagen, los corchetes son campos que se escriben o se eligen, y las casillas son opciones que se marcan.

### 1. Lista de campañas

Es el punto de entrada: muestra todas las campañas de la tienda con su estado y permite crear, importar o exportar.

&#91;embedded content: wireframe · lista de campañas\]

- Importar JSON y Exportar: mueven campañas entre tiendas. Lo que se importa entra siempre como borrador.
- Nueva campaña: abre la vista 2.
- Filtros: por texto, por estado y por regla de audiencia.
- Columnas: nombre, estado, regla, prioridad y programación. La playlist por defecto aparece siempre y no lleva regla ni prioridad.
- Menú de cada fila: editar, duplicar, pausar o activar, exportar JSON y eliminar.

### 2. Crear o editar una campaña

Es un solo formulario con cinco bloques que siguen el orden del recorrido, y un resumen fijo a la derecha.

&#91;embedded content: wireframe · crear o editar campaña\]

- Datos: nombre, prioridad y el tiempo mínimo que la campaña permanece en pantalla antes de poder cambiar.
- Audiencia: se elige una regla de la biblioteca. "+ Nueva regla" abre la vista 3 y al volver conserva lo que se llevaba hecho.
- Vetos: el sistema muestra los que aplican por las etiquetas de los medios y deja agregar otros a mano.
- Contenido: los medios se eligen de la biblioteca (vista 5), con orden y duración por pieza.
- Programación y alcance: fechas, días, franja horaria y kioskos donde corre.
- Resumen: panel fijo que se actualiza mientras se edita. Muestra estado, etiquetas, vetos que aplican, avisos y una vista previa.
- Guardar borrador siempre está disponible. Activar solo se habilita si no faltan medios ni regla.

### 3. Reglas de audiencia

Es una biblioteca de reglas reutilizables: la lista a la izquierda y el editor de la regla seleccionada a la derecha.

&#91;embedded content: wireframe · reglas de audiencia\]

- Lista: se busca y se selecciona. Cada regla indica en cuántas campañas se usa.
- Personas y edad por rango: mínimo y máximo. Una celda vacía significa sin restricción.
- Género y expresión: opcionales. La expresión viene desactivada por defecto.
- Confianza mínima: si un atributo no llega a ese umbral cuenta como desconocido y no activa la campaña.
- Aviso inferior: editar una regla cambia todas las campañas que la usan. Propuesta: una regla en uso no se puede eliminar hasta que ninguna campaña la use.

### 4. Vetos

La vista mantiene la misma estructura que las reglas. El ejemplo muestra un veto del sistema, que es el caso con más restricciones.

&#91;embedded content: wireframe · vetos\]

- Origen: Sistema (mínimo obligatorio, no se puede quitar ni desactivar) o Tienda (lo crea el administrador). Falta decidir quién define los vetos del sistema.
- Condición: lo que debe cumplirse en el grupo para que el veto se active. Por ahora es un umbral de edad; otras condiciones quedan por definir.
- Edad no fiable: para vetos se recomienda contarla como posible menor.
- Qué bloquea: etiquetas de contenido. Cuando el veto se activa, queda bloqueada cualquier campaña con medios que lleven esa etiqueta.
- Alcance: toda la tienda o un kiosko.
- Campañas que bloquea: muestra qué campañas llevan hoy esas etiquetas, para que el administrador entienda el efecto del veto.

### 5. Biblioteca de medios y carga de fliers

Aquí se suben y se etiquetan los fliers antes de usarlos en una campaña. El ejemplo tiene seleccionado un flyer vertical, que no encaja con la pantalla.

&#91;embedded content: wireframe · biblioteca de medios\]

- Zona de carga: se arrastran o se eligen varios archivos a la vez. Al subir se revisan formato, resolución y orientación.
- Filtros: por texto, tipo (imagen o video) y etiqueta.
- Tarjeta: miniatura, nombre, tipo y resolución, etiqueta y en cuántas campañas se usa. Un medio sin uso se puede eliminar sin consecuencias.
- Detalle: nombre, etiquetas de contenido (las que usan los vetos) y duración sugerida. La resolución es solo de lectura.
- Aviso de pantalla: si el medio no encaja con la pantalla del kiosko, se avisa pero no se impide guardarlo. El aviso reaparece en el resumen de la campaña.
- Propuesta: un medio en uso no se puede eliminar hasta quitarlo de las playlists.

### 6. Inicio de sesión

Una sola cuenta, sin registro. La vista tiene dos estados: el ingreso normal y el cambio obligatorio de contraseña en el primer ingreso.

&#91;embedded content: wireframe · inicio de sesión\]

- Un solo usuario y sin registro: el usuario inicial se crea al instalar el servidor.
- Primer ingreso: obliga a cambiar la contraseña inicial antes de entrar al portal.
- Errores: el mensaje no dice cuál de los dos datos falló. Tras varios intentos fallidos se bloquea el ingreso por unos minutos (número de intentos y tiempo por definir).
- Recuperación: no hay correo ni registro, así que la contraseña se restablece desde el servidor con un comando. Debe quedar en el manual de instalación.
- Sesión: se cierra por inactividad (tiempo por definir) y desde el menú Admin, opción Cerrar sesión.
- Acceso solo desde la red de la tienda. El portal no se expone a internet.
- Con una cuenta compartida no se sabe quién hizo cada cambio. Es aceptable en el MVP, pero hay que decirlo.

### 7. Estado de los kioskos

Muestra de un vistazo si el servidor, la cámara y la pantalla de cada kiosko funcionan, y qué revisar cuando algo falla.

&#91;embedded content: wireframe · estado de los kioskos\]

- Resumen superior: kioskos en línea, estado del servidor y problemas activos.
- Servidor: backend, base de datos, bus de eventos y espacio en disco.
- Tabla de kioskos: estado general (En línea, Con problema o Sin conexión), estado de cámara, visión y pantalla, campaña que se muestra y tiempo desde la última señal. Al elegir una fila se abre su detalle.
- Detalle: eventos recientes y qué revisar para esa falla. Cuando visión no recibe frames, el kiosko pasa a la playlist por defecto.
- La vista se actualiza sola cada pocos segundos.
- Indicador en el menú (propuesta): "● 1 problema" visible en todas las vistas, para ver una falla sin entrar a Estado.
- Límite que hay que decir con honestidad: "Pantalla OK" significa que el player responde, no que el televisor esté encendido. "Cámara OK" significa que llegan frames, no que el lente esté despejado.
- Si el portal no carga, la causa probable es el servidor caído, y el portal no puede mostrarlo. La alerta desde afuera llega con la nube (Fase 2).

## Consideraciones adicionales

Además del inicio de sesión y del estado de los kioskos, estas funciones cambian lo que el administrador necesita para operar y conviene decidirlas ahora.

| Consideración | Por qué importa | Cuándo |
| --- | --- | --- |
| Pausa de emergencia | Un botón "Mostrar solo la playlist por defecto" retira un anuncio problemático al instante, sin tener que editar campañas | MVP |
| Playlist por defecto obligatoria | Sin ella el kiosko podría quedar en blanco cuando no coincide ninguna campaña o falla la cámara. Avisar si falta | MVP |
| Configuración de kioskos | Nombre, ubicación, tipo de cámara (USB, CSI o RTSP) y pantalla. Sin esto, la vista Estado no distingue un kiosko de otro | MVP |
| Zona de interés de la cámara | El servicio resume un solo grupo dentro de un rectángulo configurable. El administrador necesita ajustarlo en una vista de calibración que muestre la cámara solo mientras está abierta, sin grabar ni guardar nada | MVP |
| Hora y zona horaria del servidor | Las programaciones dependen del reloj del servidor. Mostrarlo y avisar si está desajustado | MVP |
| Espacio de almacenamiento | Los medios ocupan disco. Mostrar el uso y avisar cuando se acerque al límite | MVP, dentro de Estado |
| Reportes | Ya definidos en el documento de arquitectura (conteos, dwell time, reproducciones por campaña) pero sin maquetar. Es la siguiente vista a diseñar | MVP |
| Vistas para tablet | El administrador usa laptop o tablet, así que las vistas deben funcionar en pantalla táctil y ancho reducido | MVP |
| Aviso de privacidad | Texto visible y política de tratamiento de datos. La vista de calibración y los reportes deben recordar que no se guardan imágenes | Antes del piloto |
| Respaldo y restauración | Todo vive en un solo PC y, si falla el disco, se pierden campañas y medios. "Exportar todo" en un ZIP en el MVP; copia automática de la base de datos después | Exportar en MVP, automático en Fase 1 |
| Simulador de reglas | Probar un grupo inventado (un adulto y un bebé) antes de activar una campaña | Fase 1 |
| Calendario semanal | Ver la cobertura de campañas por franja horaria, los conflictos y los huecos que caen a la playlist por defecto | Fase 1 |
| Registro de cambios | Con una sola cuenta, qué se cambió y cuándo. Permite revisar y deshacer errores | Fase 1 |
| Actualizaciones de software | Cómo se actualizan el servidor y los kioskos sin visitar cada tienda | Fase 1 o 2 |
| Cuentas por persona y alertas externas | Roles por empleado y avisos por correo o mensaje cuando un kiosko cae. Requieren un observador fuera de la tienda | Fase 2, con la nube |

## Decisiones a validar

Varias de las propuestas de este documento cambian el esquema de campaña del documento de arquitectura, así que conviene confirmarlas antes de construir.

| Tema | Propuesta | Efecto en el esquema o el alcance |
| --- | --- | --- |
| Reglas reutilizables | Una regla se crea una vez y varias campañas la usan | La audiencia deja de ser un bloque dentro de la campaña y pasa a ser una referencia. Al exportar el JSON se incrusta la regla |
| Etiquetas de contenido | Medios y campañas llevan etiquetas, y los vetos bloquean por etiqueta | Se agregan etiquetas a medios, campañas y vetos. Hoy el esquema solo guarda una lista de nombres de vetos |
| Vetos obligatorios | Un mínimo del sistema que la tienda no puede quitar | Pendiente la decisión de quién lo define, ya anotada como riesgo en el documento de arquitectura |
| Dwell time en reglas | Que una regla pueda exigir que el grupo lleve cierto tiempo frente al kiosko | Hoy las condiciones son solo de composición. Sería un campo nuevo |
| Una regla por campaña | Para atender dos audiencias se crean dos campañas | Mantiene simple el motor. Un "o" entre reglas queda fuera del MVP |
| Roles | En el MVP hay un solo administrador con login | Los roles por tienda llegan con la nube |
| Simulador | Probar una regla con un grupo inventado, por ejemplo un adulto y un bebé, antes de activar | Vista extra opcional. Usa el mismo motor de reglas |
| Formatos y tamaño de medios | Definir formatos aceptados, resolución y peso máximo | Afecta la subida y el almacenamiento |
