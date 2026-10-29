package educonnect;

import org.apache.camel.builder.RouteBuilder;

public class MatriculasRoute extends RouteBuilder {

    private final ProcesadorMatriculas procesador = new ProcesadorMatriculas();

    @Override
    public void configure() {
        from("file:input?noop=true&include=.*\\.csv&charset=UTF-8")
                .routeId("ruta-matriculas")
                .log("Archivo recibido: ${file:name}")
                .bean(procesador, "iniciar")
                .split(body().tokenize("\n"))
                    .filter(simple("${exchangeProperty.CamelSplitIndex} > 0 && ${body.trim()} != ''"))
                        .setHeader("error", method(procesador, "validar"))
                        .choice()
                            .when(header("error").isNull())
                                .setBody(method(procesador, "obligacion"))
                                .log("VALIDO     -> ${body}")
                                .bean(procesador, "agregarObligacion")
                            .otherwise()
                                .log("RECHAZADO ${header.error} <- ${body}")
                                .bean(procesador, "agregarError")
                        .end()
                    .end()
                .end()
                .setHeader("validos", method(procesador, "validos"))
                .setHeader("rechazados", method(procesador, "rechazados"))
                .setHeader("leidos", method(procesador, "leidos"))
                .setBody(method(procesador, "csvObligaciones"))
                .to("file:output?fileName=obligaciones_financieras.csv&charset=UTF-8")
                .setBody(method(procesador, "csvErrores"))
                .to("file:output?fileName=errores_matriculas.csv&charset=UTF-8")
                .log("RESUMEN archivo=${file:onlyname} leidos=${header.leidos} validos=${header.validos} rechazados=${header.rechazados}");
    }
}
