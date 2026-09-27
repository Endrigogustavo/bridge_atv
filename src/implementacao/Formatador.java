package implementacao;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Utilidades de formatação compartilhadas pelos exportadores (uso interno do pacote). */
final class Formatador {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formatador() { }

    /** Converte uma célula em texto no padrão brasileiro (1.234,56). */
    static String texto(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof Integer || valor instanceof Long) {
            return NumberFormat.getIntegerInstance(PT_BR).format(valor);
        }
        if (valor instanceof BigDecimal decimal) {
            return decimal(decimal, Math.max(decimal.scale(), 0));
        }
        if (valor instanceof Number numero) {
            return decimal(BigDecimal.valueOf(numero.doubleValue()), 2);
        }
        return valor.toString();
    }

    static boolean numerico(Object valor) {
        return valor instanceof Number;
    }

    /** Carimbo que prova, dentro do próprio arquivo, qual par abstração/implementação o gerou. */
    static String carimbo(Documento documento, Exportador exportador) {
        return "Gerado em " + documento.geradoEm().format(DATA_HORA)
                + " · Abstração: " + documento.origem()
                + " · Implementação: " + exportador.getClass().getSimpleName();
    }

    private static String decimal(BigDecimal valor, int casas) {
        NumberFormat nf = NumberFormat.getNumberInstance(PT_BR);
        nf.setMinimumFractionDigits(casas);
        nf.setMaximumFractionDigits(casas);
        return nf.format(valor);
    }
}
