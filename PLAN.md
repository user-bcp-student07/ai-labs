# Plan

## api

### Alcance y preservacion

- H-API-01 a H-API-07 estan marcados `Aprobado` en [HALLAZGOS.md](./HALLAZGOS.md)
  y quedan incluidos en el alcance aprobado de este plan. Se seleccionan los
  siete por contar con aprobacion; la matriz no contiene hallazgos api
  `Pendiente` en este momento.
- `docs/ENDPOINTS.md` no existe. Se toma como referencia el contrato observado
  anotado en HALLAZGOS.md: rutas, metodos, campos de respuesta, codigos de
  estado, campo multipart `file` y encabezado `Authorization: Bearer`.
- Mantener esos contratos HTTP y sus formas de respuesta. Los cambios de
  seguridad restringiran comportamientos inseguros —por ejemplo, consultar
  tareas ajenas, aceptar tokens no verificados o escribir fuera de uploads— y
  no se consideran compatibilidad que deba conservarse.
- No se modificara codigo como parte de este plan. Antes de implementar, escribir
  y ejecutar pruebas de caracterizacion en `api/tests/test_api.py`; registrar
  resultados reales y coordinar cambios de emisor de token con el responsable
  externo de autenticacion. No se inspecciono codigo fuera de `api/`.
- Cambios estructurales propuestos en `api/app/` son archivos nuevos y
  `api/app/main.py`; confirmar la organizacion durante la implementacion.

### H-API-01 — Token Base64 con secreto embebido

- **Hallazgo:** H-API-01, token Base64 con secreto embebido.
- **Estado y alcance:** `Aprobado` en HALLAZGOS.md. Los siete apartados api de
  este documento estan incluidos en el alcance aprobado.
- **Problema:** Base64 no autentica el contenido. El secreto fijo permite
  fabricar tokens y no se validan firma, emisor ni audiencia; los errores
  internos de validacion se devuelven al cliente.
- **Cambio propuesto:** Verificar JWT firmado y validar algoritmo permitido,
  firma, emisor, audiencia y expiracion con configuracion externa. No incluir
  secretos reales ni aceptar el formato Base64 como fallback, porque ese
  fallback anularia la verificacion. Mantener el encabezado Bearer y las
  claims de identidad/rol necesarias para el API; coordinar antes el emisor
  externo para evitar una incompatibilidad de tokens. Usar mensajes 401
  genericos sin texto de excepcion. Justificar cualquier dependencia JWT
  nueva por verificacion criptografica mantenida, revisar sus advisories y
  fijar una version corregida antes de incorporarla.
- **Archivos a tocar:** `api/app/main.py`; nuevos modulos de seguridad y
  configuracion bajo `api/app/`; `api/requirements.txt` solo si la evaluacion
  justifica una dependencia JWT; `api/tests/test_api.py`.
- **Caracterizacion previa:** Con el estado actual, probar token valido con
  identidad/rol y acceso exitoso; token vencido, malformado y secreto
  incorrecto responden 401; registrar la forma actual del error sin convertirla
  en contrato objetivo. Despues, agregar pruebas del JWT firmado y de rechazos
  por firma, emisor, audiencia y expiracion, usando claves aisladas de prueba.

### H-API-02 — Autorizacion permite consultar o crear para otro `owner`

- **Hallazgo:** H-API-02, el cliente puede seleccionar `owner` en consultas y
  creaciones.
- **Problema:** `GET /api/tasks`, `GET /api/summary` y `POST /api/tasks`
  permiten que la identidad autenticada lea datos o asigne tareas a otro
  propietario.
- **Cambio propuesto:** Derivar siempre el propietario de `identity` verificada.
  Ignorar o rechazar el `owner` recibido del cliente de forma consistente, sin
  conceder acceso adicional por rol no documentado. Mantener las rutas y formas
  de respuesta actuales; `owner` en las respuestas sera la identidad autorizada.
- **Archivos a tocar:** `api/app/main.py`; futuros servicios/repositorios de
  tareas bajo `api/app/`; `api/tests/test_api.py`.
- **Caracterizacion previa:** Comprobar y registrar que un `owner` de query
  distinto selecciona actualmente otro conjunto en `/api/tasks` y `/api/summary`,
  y que el `owner` del cuerpo puede asignar una tarea a otro usuario. Incluir
  tambien el comportamiento predeterminado cuando no se envia `owner`.
  Despues, probar que las tres operaciones quedan ligadas a la identidad del
  token y que no filtran ni asignan datos a otro propietario.

### H-API-03 — Filtros e inserts SQL concatenados

- **Hallazgo:** H-API-03, filtros e inserts SQL concatenados.
- **Problema:** `owner` y los campos de tareas se interpolan en SQL; la consulta
  por ID tambien concatena el valor. Esto expone las operaciones a alteracion
  de la consulta por entradas y puede causar fallos con valores validos que
  contienen comillas.
- **Cambio propuesto:** Trasladar las consultas a un repositorio SQLite y usar
  parametros enlazados en filtros, inserts y seleccion por ID. No interpolar
  valores de entrada en SQL. Mantener columnas, orden, valores por defecto,
  persistencia y formato de salida observados.
- **Archivos a tocar:** `api/app/main.py`; nuevo repositorio bajo `api/app/`;
  `api/tests/test_api.py`.
- **Caracterizacion previa:** Probar listado filtrado, resumen, creacion,
  valores por defecto y serializacion existente con datos ordinarios y valores
  que incluyan comillas. Verificar el `id` y que las operaciones no alteren
  otras filas. La prueba debe fijar resultados funcionales, no requerir que una
  entrada SQL se ejecute como codigo. Tras parametrizar, repetir las pruebas y
  verificar que el texto se trata como dato.

### H-API-04 — Nombre de archivo sin normalizar ni limites de tipo y tamaño

- **Hallazgo:** H-API-04, uso directo de `upload.filename` y lectura completa.
- **Problema:** El nombre proporcionado participa en la ruta y el archivo se
  carga entero en memoria, sin validar nombre, extension ni tamano; puede
  sobrescribir destinos fuera del directorio previsto o agotar recursos.
- **Cambio propuesto:** Normalizar y validar el nombre, restringir extensiones
  permitidas y limitar el tamano durante la lectura, antes de escribir.
  Conservar `POST /api/requirements`, el campo `file`, estado 201 y las claves
  de respuesta (`owner`, `fileName`, `storedAt`, `size`). El nombre/ruta
  almacenados seran seguros; decidir y documentar el valor de `fileName`
  compatible con el formato observado sin usarlo como ruta. Centralizar
  extensiones y limite en constantes con nombre.
- **Archivos a tocar:** `api/app/main.py`; nuevo servicio/utilidad de archivos
  bajo `api/app/`; `api/tests/test_api.py`.
- **Caracterizacion previa:** Probar carga valida y verificar bytes guardados,
  nombre devuelto, ruta, tamano y estado. Caracterizar por separado el nombre
  de archivo con componentes de ruta, extension no permitida y archivo que
  excede el limite propuesto; estos casos actualmente no tienen rechazo
  definido. Acordar limites antes de implementar y probar luego rechazo sin
  escritura ni lectura ilimitada.

### H-API-05 — Versiones de dependencias candidatas a revision SCA

- **Hallazgo:** H-API-05, `urllib3` y `python-multipart` fijadas a las
  versiones listadas en el reto.
- **Problema:** La presencia de versiones concretas no demuestra por si sola
  un CVE aplicable. La ausencia de verificacion puede dejar una dependencia
  vulnerable o motivar una actualizacion innecesaria/incompatible.
- **Cambio propuesto:** Revisar advisories oficiales y el arbol resuelto para
  `urllib3==2.5.0` y `python-multipart==0.0.21`; registrar identificadores,
  rango afectado y version corregida antes de cambiar versiones. Si hay CVE
  aplicable, actualizar a una version corregida compatible y ejecutar pruebas
  y scanner; si no, conservar el pin y registrar evidencia. No agregar
  dependencias sin justificacion ni silenciar alertas.
- **Archivos a tocar:** `api/requirements.txt` solo si la evidencia requiere
  cambio; `api/tests/test_api.py` solo si se necesitan pruebas de compatibilidad
  de carga; evidencia de hallazgos en `HALLAZGOS.md`.
- **Caracterizacion previa:** Antes de modificar dependencias, registrar
  versiones instaladas/resueltas y ejecutar la suite existente como baseline.
  Probar carga multipart valida para caracterizar integracion de
  `python-multipart`; para `urllib3`, caracterizar la respuesta observable de
  `/api/backend-health` con respuesta HTTP y fallo de conexion simulados.
  No se afirma ni se registra CVE confirmado hasta completar el analisis.

### H-API-06 — Tipado debil, excepciones amplias y logica compleja

- **Hallazgo:** H-API-06, uso de diccionarios sin modelos, capturas amplias,
  anidamiento y duplicacion.
- **Problema:** `dict[str, Any]` deja entradas/salidas sin validacion de esquema;
  las excepciones amplias ocultan causas y pueden exponer detalles al cliente;
  condicionales anidados y transformaciones mezcladas dificultan el
  mantenimiento.
- **Cambio propuesto:** Definir modelos Pydantic de request/response con los
  campos y alias observados, incluidos valores por defecto actuales. Separar
  validacion, conversion, reglas y respuesta en funciones pequenas, con
  nombres por proposito, guard clauses y helpers reutilizables. Convertir
  errores esperados a respuestas HTTP genericas y no devolver `str(exception)`.
  No introducir validaciones de negocio que alteren el contrato sin acordarlas.
- **Archivos a tocar:** `api/app/main.py`; nuevos modelos/servicios bajo
  `api/app/`; `api/tests/test_api.py`.
- **Caracterizacion previa:** Cubrir claves y tipos de todas las respuestas,
  valores por defecto de `POST /api/tasks`, coerciones de campos observadas,
  resumen por estado/prioridad, respuestas 401 y respuestas de
  `/api/backend-health`. Despues, verificar los mismos formatos y resultados
  para entradas validas; anadir casos Pydantic para entradas invalidas y
  mensajes de error genericos, documentando cualquier cambio deliberado.

### H-API-07 — Rutas, reglas, persistencia y serializacion en un archivo

- **Hallazgo:** H-API-07, responsabilidades de la aplicacion concentradas en
  `api/app/main.py`.
- **Problema:** La inicializacion SQLite, rutas, SQL, reglas de resumen y
  serializacion conviven en el mismo modulo; cambios y pruebas quedan
  acoplados.
- **Cambio propuesto:** Separar incrementalmente las rutas FastAPI, servicios
  de reglas y repositorio SQLite, manteniendo `create_app` como punto de
  composicion y el cierre de conexion. Reutilizar modelos/servicios del
  H-API-06 y repositorio del H-API-03; evitar duplicar consultas o mapeos.
  Mantener rutas, codigos de estado, datos iniciales y formas de respuesta
  observadas.
- **Archivos a tocar:** `api/app/main.py`; nuevos modulos de routers, servicios,
  repositorios y modelos bajo `api/app/`; `api/tests/test_api.py`.
- **Caracterizacion previa:** Antes de mover responsabilidades, fijar pruebas
  de todos los endpoints actuales: lista inicial y filtrada, creacion,
  resumen, carga de requerimiento, salud del backend, autenticacion y cierre
  de aplicacion. Comparar metodo, ruta, estado, claves y valores de respuesta.
  Repetir las mismas pruebas tras cada extraccion para detectar regresiones.

### Secuencia y controles

1. Escribir primero las pruebas de caracterizacion indicadas y ejecutar baseline
   desde `api/` con `pytest`; guardar solo resultados realmente obtenidos.
2. Para H-API-01, acordar el contrato del token con el emisor externo antes de
   cambiar el formato; no implementar hasta resolver esta dependencia externa.
3. Implementar H-API-01 a H-API-07 en incrementos revisables, una vez
   satisfechos los requisitos externos aplicables.
4. Ejecutar pruebas dirigidas y regresion desde `api/`; buscar cobertura de
   85% o superior para codigo modificado, sin reducir gates ni inventar
   resultados.
5. Actualizar estados y evidencias por ID en `HALLAZGOS.md` solo cuando haya
   correccion y validacion comprobadas. No se infiere aprobacion adicional.
