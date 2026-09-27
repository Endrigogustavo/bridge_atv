package abstracao;

import implementacao.Documento;
import implementacao.Documento.Bloco;
import implementacao.Exportador;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * ABSTRAÇÃO do padrão Bridge.
 *
 * <p>Representa o lado "O QUE relatar". Mantém uma referência para o
 * {@link Exportador} (a <b>ponte</b>) e delega a ele todo o trabalho de
 * formato. A referência é do tipo da INTERFACE: esta classe nunca conhece
 * PDF, Excel ou HTML.</p>
 *
 * <p><b>Injeção de dependência:</b> o exportador chega pronto pelo construtor
 * e pode ser substituído em tempo de execução por {@link #trocarExportador}.
 * Nenhuma classe deste pacote instancia um exportador concreto.</p>
 *
 * <p>{@link #gerar()} é um <i>Template Method</i> final: toda subclasse segue o
 * mesmo fluxo (montar conteúdo → atravessar a ponte → empacotar o arquivo)
 * e só decide o conteúdo.</p>
 */
public abstract class Relatorio {

    protected static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /** A ponte: aponta para o lado da implementação. */
    private Exportador exportador;

    protected Relatorio(Exportador exportador) {
        this.exportador = exigir(exportador);
    }

    /** Troca o formato de saída sem recriar o relatório (composição > herança). */
    public final void trocarExportador(Exportador novoExportador) {
        this.exportador = exigir(novoExportador);
    }

    public final Exportador exportador() {
        return exportador;
    }

    /** Fluxo único de geração, igual para qualquer relatório e qualquer formato. */
    public final ArquivoExportado gerar() {
        Documento documento = new Documento(
                titulo(), subtitulo(), getClass().getSimpleName(), LocalDateTime.now(), montarConteudo());

        byte[] conteudo = exportador.renderizar(documento);   // ← travessia da ponte

        return new ArquivoExportado(nomeBase() + "." + exportador.extensao(), exportador.formato(), conteudo);
    }

    // ---------------------------------------------- ganchos das abstrações refinadas

    protected abstract String titulo();

    protected abstract String subtitulo();

    protected abstract String nomeBase();

    /** Descreve O QUE o relatório mostra, apenas com blocos neutros de formato. */
    protected abstract List<Bloco> montarConteudo();

    // ---------------------------------------------------------------- utilidades

    private static Exportador exigir(Exportador exportador) {
        return Objects.requireNonNull(exportador, "Um Exportador deve ser injetado no relatório.");
    }

    protected static String moeda(BigDecimal valor) {
        return "R$ " + decimal(valor, 2);
    }

    protected static String decimal(BigDecimal valor, int casas) {
        NumberFormat nf = NumberFormat.getNumberInstance(PT_BR);
        nf.setMinimumFractionDigits(casas);
        nf.setMaximumFractionDigits(casas);
        return nf.format(valor.setScale(casas, RoundingMode.HALF_EVEN));
    }

    protected static String percentual(double fracao) {
        return decimal(BigDecimal.valueOf(fracao * 100), 1) + "%";
    }

    protected static BigDecimal arredondar(double valor, int casas) {
        return BigDecimal.valueOf(valor).setScale(casas, RoundingMode.HALF_EVEN);
    }

    /** "3º trimestre 2026" → "3-trimestre-2026". */
    protected static String slug(String texto) {
        String semAcento = Normalizer.normalize(texto.toLowerCase(PT_BR), Normalizer.Form.NFD)
                                     .replaceAll("\\p{M}", "");
        return semAcento.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
