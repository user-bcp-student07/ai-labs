---
name: feature-work-test
description: 'Estandariza la definición de una automatización de pruebas diarias que genera un listado del estado de salud de aplicaciones del grupo ABCD en desarrollo y certificación. Úsalo antes de planificar o implementar esta funcionalidad.'
---

# Skill: Automatización diaria de pruebas y estado de salud

## 1) Metadata
- **Skill ID:** feature-work-test
- **Version:** 0.1.0
- **Purpose:** Preparar una especificación trazable para automatizar una tarea diaria de pruebas y generar un listado con el estado de salud de las aplicaciones del grupo ABCD en los ambientes de desarrollo y certificación.
- **Audience (roles):** Chapter/PO, Tech Lead, Backend, Frontend, QA, Cyber
- **When to use:** Antes de planificar o implementar la automatización diaria y su listado de estado.
- **When NOT to use:** Incidentes P1 de producción, cambios triviales o tareas sin relación con esta funcionalidad.
- **Sensitive inputs:** No incluyas datos reales de clientes, credenciales, secretos, tokens ni URLs internas no autorizadas.
- **Expected output:** Un documento Markdown estructurado con las nueve secciones indicadas en Output.

## 2) Inputs
Usa la solicitud y el material proporcionado como fuente. Registra como confirmado únicamente lo que allí se indique. Para información no especificada, formula preguntas abiertas; no completes los vacíos con requisitos supuestos.

- **Contexto de negocio:**
- **Solicitud / historia de usuario:** Automatizar una tarea de pruebas diaria que genere un listado con el estado de salud de las aplicaciones del grupo ABCD en los ambientes de desarrollo y certificación.
- **Aplicaciones incluidas y responsables:**
- **Definición de estado de salud:** (pruebas o señales consideradas, estados posibles y tratamiento de errores)
- **Fuentes de datos y dependencias:**
- **Horario y zona horaria de ejecución:**
- **Formato, destino y audiencia del listado:**
- **Restricciones técnicas y estándares:**
- **Riesgos conocidos:**
- **Definition of Done del equipo:**

Los campos sin respuesta no son requisitos: deben aparecer como preguntas abiertas.

## 3) Output
Devuelve **solo** un documento Markdown breve con estas nueve secciones:

1. **Summary**
2. **Scope**
   - Dentro del alcance
   - Fuera del alcance
3. **Supuestos**
4. **Decisiones técnicas por tomar (pendientes o resueltas)**
5. **Riesgos y mitigaciones**
6. **Criterios de aceptación (medibles)**
7. **Plan de validación**
   - Pruebas unitarias
   - Pruebas funcionales
   - Seguridad (comprobaciones mínimas)
   - Evidencia requerida (qué adjuntar)
8. **Tareas sugeridas por rol**
   - Chapter/PO
   - Tech Lead
   - Backend
   - Frontend
   - QA
   - Cyber
9. **Preguntas abiertas**

Redacta la salida en español, salvo que el usuario solicite otro idioma. Mantén los nombres y el orden de las secciones.

## 4) Guidance
- Trata como alcance confirmado la ejecución diaria automatizada, el grupo ABCD y los ambientes de desarrollo y certificación.
- No inventes el inventario de aplicaciones, horario, zona horaria, criterios o umbrales de salud, fuentes de datos, mecanismo de ejecución, formato, destino ni destinatarios del listado.
- Distingue entre una prueba automatizada y una comprobación de salud del ambiente. Si la solicitud no aclara qué debe ejecutarse ni cómo se determina la salud, consígnalo en **Preguntas abiertas**.
- No presentes una aplicación o ambiente como saludable cuando falte información o falle la consulta. Registra el tratamiento de esos casos como decisión pendiente, salvo que la entrada ya lo defina.
- Formula criterios verificables sobre la ejecución diaria y la cobertura de aplicaciones y ambientes confirmados. No establezcas objetivos de disponibilidad, tiempos, tasas de éxito o reintentos que no hayan sido proporcionados.
- Incluye en **Riesgos y mitigaciones** al menos dos pares concretos relacionados con inventario incompleto, definición ambigua de salud, resultados obsoletos o fallas de ejecución; adapta o elimina los que no correspondan según la entrada.
- Propón tareas por rol de acuerdo con sus responsabilidades. Para roles que no apliquen, indícalo y explica por qué; no asignes implementación de UI si no está solicitada.
- No propongas herramientas ni dependencias fuera del stack permitido o no confirmadas en la entrada.
- No incluyas información sensible ni secretos.
- Mantén la salida en un máximo de 1-2 páginas.

## 5) Validation Rules
La salida es válida si:
- Incluye las nueve secciones, en el orden establecido.
- Tiene al menos tres criterios de aceptación comprobables, sin introducir umbrales o políticas no confirmados.
- Tiene al menos tres tareas para cada rol central: Tech Lead, Backend y QA, o explica por qué un rol no aplica.
- Incluye al menos dos riesgos con mitigaciones.
- Refleja la frecuencia diaria y los dos ambientes solicitados sin inventar el horario.
- Marca como pregunta abierta cada decisión relevante que la solicitud no confirme.
- El plan de validación incluye evidencia concreta, por ejemplo, resultados de ejecución y el listado generado.
- No contiene datos sensibles ni secretos.
