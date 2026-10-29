# Progreso 1 - Integración SA → SF (EduConnect) con Apache Camel

Lee las matrículas exportadas por el Sistema Académico (`input/*.csv`), valida cada registro,
aplica la regla de becas y genera:

- `output/obligaciones_financieras.csv` - registros válidos en formato del Sistema Financiero.
- `output/errores_matriculas.csv` - registros rechazados con su `motivo_error`.

## Requisitos

- Java 17 o superior (probado con OpenJDK 21)
- Maven 3.x
- Apache Camel 4.23.0 (Maven lo descarga desde `pom.xml`)

## Ejecución

Desde la carpeta del proyecto:

```bash
mvn compile exec:java
```

La aplicación procesa los CSV de `input/`, escribe las salidas en `output/` y se detiene sola
tras 5 s sin trabajo. Para guardar el log como evidencia:

```bash
mvn compile exec:java 2>&1 | tee ejecucion.log
```

Resumen esperado en el log:

```
RESUMEN archivo=matriculas.csv leidos=10 validos=6 rechazados=4
```

Para probar otro archivo, reemplace `input/matriculas.csv` (mismo esquema) y vuelva a ejecutar.

## Diseño

| Clase                  | Rol                                                                             |
| ---------------------- | ------------------------------------------------------------------------------- |
| `App`                  | Arranca Camel Main en modo lote (`durationMaxIdleSeconds=5`).                   |
| `MatriculasRoute`      | Ruta: File consumer → Splitter → Filter → Content-Based Router → File producer. |
| `ProcesadorMatriculas` | Reglas de negocio: validación, duplicados, cálculo con `BigDecimal` HALF_UP.    |

- `noop=true`: el CSV original no se mueve ni modifica.
- Splitter secuencial: se conserva el orden de entrada.
- Un registro inválido solo se reporta en errores; el resto se sigue procesando.
- Prioridad de errores: `ID_INVALIDO`, `ID_DUPLICADO`, `NOMBRE_VACIO`, `CARRERA_VACIA`,
  `VALOR_INVALIDO`, `BECA_INVALIDA`, `ESTADO_INVALIDO`, `ESTUDIANTE_RETIRADO`.
- Si `input/` contiene varios CSV, cada uno sobrescribe las salidas; el último procesado queda en `output/`.
