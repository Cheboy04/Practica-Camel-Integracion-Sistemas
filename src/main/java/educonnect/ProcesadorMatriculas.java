package educonnect;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.camel.Header;

public class ProcesadorMatriculas {

    // Columnas del CSV de entrada
    static final int ID = 0, NOMBRE = 1, CARRERA = 2, VALOR = 3, BECA = 4, ESTADO = 5;

    private final Set<Long> idsReservados = new HashSet<>();
    private final List<String> obligaciones = new ArrayList<>();
    private final List<String> errores = new ArrayList<>();

    // Se llama al inicio de cada archivo: cada archivo empieza sin IDs ni resultados previos.
    public void iniciar() {
        idsReservados.clear();
        obligaciones.clear();
        errores.clear();
    }

    public void agregarObligacion(String lineaFinanciera) {
        obligaciones.add(lineaFinanciera);
    }

    // Conserva id y nombre tal como venían, aunque estén vacíos.
    public void agregarError(String linea, @Header("error") String error) {
        String[] p = linea.strip().split(",", -1);
        errores.add(String.join(",", p[0], p.length > 1 ? p[1] : "", error));
    }

    public int leidos() {
        return obligaciones.size() + errores.size();
    }

    public int validos() {
        return obligaciones.size();
    }

    public int rechazados() {
        return errores.size();
    }

    public String csvObligaciones() {
        return csv("estudiante_id,nombre,concepto,valor_original,descuento,total", obligaciones);
    }

    public String csvErrores() {
        return csv("id_estudiante,nombre,motivo_error", errores);
    }

    private static String csv(String cabecera, List<String> filas) {
        StringBuilder sb = new StringBuilder(cabecera).append('\n');
        for (String fila : filas) {
            sb.append(fila).append('\n');
        }
        return sb.toString();
    }

    // Devuelve el código de error del registro, o null si es válido.
    public String validar(String linea) {
        String[] c = campos(linea);

        Long id = parsearId(c[ID]);
        if (id == null) {
            return "ID_INVALIDO";                 // no reserva ID
        }
        if (!idsReservados.add(id)) {             // add() devuelve false si ya existía
            return "ID_DUPLICADO";                // prioridad sobre los demás errores
        }
        // A partir de aquí el ID ya quedó reservado, aunque la fila tenga otro error.
        if (c[NOMBRE].isEmpty()) return "NOMBRE_VACIO";
        if (c[CARRERA].isEmpty()) return "CARRERA_VACIA";
        if (parsearValor(c[VALOR]) == null) return "VALOR_INVALIDO";
        if (!c[BECA].equals("S") && !c[BECA].equals("N")) return "BECA_INVALIDA";
        if (!c[ESTADO].equals("MATRICULADO") && !c[ESTADO].equals("RETIRADO")) return "ESTADO_INVALIDO";
        if (c[ESTADO].equals("RETIRADO")) return "ESTUDIANTE_RETIRADO";
        return null;
    }

    // Transforma un registro YA VALIDADO a la línea del formato financiero:
    // estudiante_id,nombre,concepto,valor_original,descuento,total
    public String obligacion(String linea) {
        String[] c = campos(linea);

        BigDecimal original = parsearValor(c[VALOR]).setScale(2, RoundingMode.HALF_UP);
        BigDecimal porcentaje = c[BECA].equals("S") ? new BigDecimal("0.15") : BigDecimal.ZERO;
        BigDecimal descuento = original.multiply(porcentaje).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = original.subtract(descuento);   // ambos con 2 decimales: resta exacta

        return String.join(",", c[ID], c[NOMBRE], "MATRICULA",
                original.toPlainString(), descuento.toPlainString(), total.toPlainString());
    }

    // Separa la línea en 6 campos sin espacios; los que falten quedan como "".
    static String[] campos(String linea) {
        String[] partes = linea.strip().split(",", -1);   // -1: conserva campos vacíos al final
        String[] c = new String[6];
        for (int i = 0; i < 6; i++) {
            c[i] = i < partes.length ? partes[i].strip() : "";
        }
        return c;
    }

    // Entero positivo o null.
    static Long parsearId(String texto) {
        if (!texto.matches("\\d{1,18}")) return null;    // solo dígitos: descarta "-5", "abc", "10.5", ""
        long id = Long.parseLong(texto);
        return id > 0 ? id : null;
    }

    // Número mayor que 0 o null.
    static BigDecimal parsearValor(String texto) {
        try {
            BigDecimal valor = new BigDecimal(texto);
            return valor.signum() > 0 ? valor : null;
        } catch (NumberFormatException e) {          // "" o texto no numérico
            return null;
        }
    }
}
