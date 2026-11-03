AMBITO
Workspace ai-labs abierto en su raiz. Lee codigo solo en api/.
Todas las rutas parten de la raiz. Lee agents.md y RETO.md y
docs/ENDPOINTS.md . Este pedido es de solo lectura; no escribas archivos.
Git se ejecuta desde la raiz; las pruebas propuestas usan cwd api/.
No leas ni modifiques codigo de auth/ ni web/. Usa IDs H-Api-NN y componente api.

CONTEXTO
una vez en el proyecto ai-labs autentica mediante POST /api/login. Lee README.md,
RETO.md y docs/ENDPOINTS.md si existe. Localiza la ruta desde el recurso
HTTP hasta la consulta a SQLite. No des por cierta la lista de RETO.md.

OBJETIVO
Determinar si entradas del login llegan concatenadas a una consulta SQL.

RESTRICCIONES
No edites codigo ni ejecutes ataques contra servicios remotos. Usa solo
codigo y datos ficticios locales. Si falta un archivo o no puedes ejecutar
una comprobacion, dilo. No cambies contratos ni leas otros componentes.

CRITERIOS DE ACEPTACION
Identifica el recorrido de la entrada hasta la consulta con archivo y linea.
Distingue concatenacion, parametrizacion y cualquier caso no comprobado.
Propone una prueba local con credenciales validas, invalidas y entrada con
comilla, con resultado esperado segun el contrato observado. No afirmes
que la prueba paso si solo la propusiste.

FORMATO DE RESPUESTA
Tabla: afirmacion | evidencia archivo:linea | comprobado o hipotesis |
prueba propuesta. Termina con preguntas abiertas y comandos no ejecutados.

