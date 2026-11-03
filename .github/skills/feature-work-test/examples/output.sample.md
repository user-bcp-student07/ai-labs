# Automatización diaria de pruebas y estado de salud

## 1. Summary
Automatizar una tarea de pruebas diaria que genere un listado con el estado de salud de las aplicaciones del grupo ABCD en los ambientes de desarrollo y certificación.

## 2. Scope
- **Dentro del alcance**
  - Ejecución diaria automatizada.
  - Aplicaciones del grupo ABCD.
  - Ambientes de desarrollo y certificación.
  - Generación de un listado con el estado de salud.
- **Fuera del alcance**
  - No especificado.

## 3. Supuestos
- No se confirman supuestos adicionales; todo lo no indicado en la solicitud figura en Preguntas abiertas.

## 4. Decisiones técnicas por tomar (pendientes o resueltas)
- **Pendiente:** inventario de aplicaciones del grupo ABCD.
- **Pendiente:** definición de estado de salud y tratamiento de aplicaciones sin información o con falla de consulta.
- **Pendiente:** mecanismo de ejecución, fuentes de datos y horario.
- **Pendiente:** formato, destino y audiencia del listado.

## 5. Riesgos y mitigaciones
- **Inventario incompleto de aplicaciones:** acordar y versionar la lista de aplicaciones por ambiente antes de implementar.
- **Definición ambigua de salud:** documentar estados y criterios aprobados por Tech Lead y QA.
- **Resultados obsoletos o ejecución fallida:** registrar fecha de ejecución y marcar explícitamente las aplicaciones sin resultado.

## 6. Criterios de aceptación (medibles)
- La tarea se ejecuta una vez por día en desarrollo y en certificación.
- El listado incluye todas las aplicaciones del inventario aprobado del grupo ABCD, en ambos ambientes.
- Cada aplicación tiene un estado de salud de los definidos y la fecha de ejecución.
- Las aplicaciones sin resultado se marcan como tales y no como saludables.

## 7. Plan de validación
- **Pruebas unitarias**
  - Clasificación de estados según la definición aprobada.
  - Tratamiento de aplicaciones sin información o con falla de consulta.
- **Pruebas funcionales**
  - Ejecución de la tarea en ambos ambientes y verificación de cobertura del listado.
- **Seguridad (comprobaciones mínimas)**
  - Verificar que el listado y los registros no contengan secretos ni datos sensibles.
- **Evidencia requerida (qué adjuntar)**
  - Salida de las pruebas y listado generado por ambiente.
  - Registro de ejecuciones diarias.

## 8. Tareas sugeridas por rol
- **Chapter/PO**
  - Confirmar el inventario de aplicaciones y la audiencia del listado.
  - Aprobar el formato y destino del listado.
  - Revisar el listado generado.
- **Tech Lead**
  - Definir el mecanismo de ejecución y las fuentes de datos.
  - Aprobar la definición de estado de salud.
  - Definir horario y zona horaria con el equipo.
- **Backend**
  - Implementar la recolección del estado por aplicación y ambiente.
  - Implementar la generación del listado.
  - Implementar el manejo de aplicaciones sin información o con falla.
- **Frontend**
  - No aplica: la solicitud no pide interfaz de usuario.
- **QA**
  - Diseñar casos para ambos ambientes.
  - Verificar cobertura y exactitud del listado.
  - Adjuntar la evidencia de ejecución.
- **Cyber**
  - Revisar el manejo de credenciales y acceso a los ambientes.
  - Verificar que no se expongan datos sensibles en el listado ni en los registros.

## 9. Preguntas abiertas
- ¿Qué aplicaciones componen el grupo ABCD en cada ambiente?
- ¿Qué pruebas o señales definen el estado de salud y cuáles son los estados posibles?
- ¿A qué hora y en qué zona horaria debe ejecutarse?
- ¿Cuál es el formato, destino y audiencia del listado?
- ¿Qué fuentes de datos y mecanismo de ejecución están permitidos?
- ¿Qué se considera fuera del alcance?