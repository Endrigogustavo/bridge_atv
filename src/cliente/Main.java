package cliente;

import abstracao.ArquivoExportado;
import abstracao.Relatorio;
import abstracao.RelatorioDesempenhoRH;
import abstracao.RelatorioVendas;
import implementacao.Exportador;
import implementacao.ExportadorExcel;
import implementacao.ExportadorHtml;
import implementacao.ExportadorPdf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * CLIENTE — script de validação do desacoplamento.
 *
 * <p>É o único ponto do sistema que conhece classes concretas dos dois lados
 * da ponte. Aqui acontece a <b>composição</b>: o cliente cria o exportador e o
 * INJETA no relatório via construtor (papel de "composition root").</p>
 *
 * <p>Rotinas exigidas:</p>
 * <ol>
 *   <li>Relatório de Vendas em PDF;</li>
 *   <li>o MESMO objeto de Relatório de Vendas trocado para Excel em tempo de execução;</li>
 *   <li>Relatório de RH em HTML.</li>
 * </ol>
 */
public final class Main {

    private static final Path SAIDA = Path.of("saida");
    private static final List<Path> GERADOS = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        Files.createDirectories(SAIDA);
        banner();

        // ── Rotina 1 ──────────────────────────────────────────────────────────
        etapa(1, "Relatório de Vendas em PDF");
        Exportador pdf = new ExportadorPdf();                           // implementação criada FORA...
        RelatorioVendas vendas = new RelatorioVendas(pdf,               // ...e injetada via construtor
                "3º trimestre 2026", DadosDemo.vendas());
        campo("abstração", id(vendas));
        campo("implementação", nome(vendas.exportador()) + "   (injetada via construtor)");
        publicar(vendas.gerar());

        // ── Rotina 2 ──────────────────────────────────────────────────────────
        etapa(2, "Troca em tempo de execução: o MESMO relatório, agora em Excel");
        String instanciaAntes = id(vendas);
        String exportadorAntes = nome(vendas.exportador());
        vendas.trocarExportador(new ExportadorExcel());                 // troca a ponte, não o relatório
        campo("implementação", exportadorAntes + "  →  " + nome(vendas.exportador()) + "   (trocarExportador)");
        campo("mesma instância?", (instanciaAntes.equals(id(vendas)) ? "SIM" : "NÃO") + " — " + id(vendas));
        publicar(vendas.gerar());

        // ── Rotina 3 ──────────────────────────────────────────────────────────
        etapa(3, "Relatório de Desempenho de RH em HTML");
        RelatorioDesempenhoRH rh = new RelatorioDesempenhoRH(new ExportadorHtml(),
                "Ciclo 2026.1", DadosDemo.avaliacoes());
        campo("abstração", id(rh));
        campo("implementação", nome(rh.exportador()) + "   (injetada via construtor)");
        publicar(rh.gerar());

        provaDeDesacoplamento();
    }

    // ---------------------------------------------------------------------------

    private static void publicar(ArquivoExportado arquivo) throws IOException {
        Path destino = SAIDA.resolve(arquivo.nome());
        Files.write(destino, arquivo.conteudo());
        GERADOS.add(destino);
        campo("formato", arquivo.formato());
        campo("arquivo", destino + "   (" + kb(arquivo.tamanho()) + ")");
        campo("assinatura", assinatura(arquivo.conteudo()));
    }

    /** Confere os "magic bytes": prova que o arquivo gerado é real, não um texto fingindo ser PDF/XLSX. */
    private static String assinatura(byte[] b) {
        if (comeca(b, "%PDF-".getBytes(StandardCharsets.US_ASCII))) {
            return new String(b, 0, 8, StandardCharsets.US_ASCII) + "  ✔ PDF válido";
        }
        if (comeca(b, new byte[] {'P', 'K', 3, 4})) {
            return "PK\\x03\\x04  ✔ pacote Office Open XML (zip)";
        }
        if (comeca(b, "<!DOCTYPE html>".getBytes(StandardCharsets.US_ASCII))) {
            return "<!DOCTYPE html>  ✔ documento HTML5";
        }
        return "✘ assinatura desconhecida";
    }

    private static void provaDeDesacoplamento() {
        List<Class<? extends Relatorio>> relatorios = List.of(RelatorioVendas.class, RelatorioDesempenhoRH.class);
        List<Class<? extends Exportador>> exportadores =
                List.of(ExportadorPdf.class, ExportadorExcel.class, ExportadorHtml.class);
        int n = relatorios.size(), m = exportadores.size();

        System.out.println();
        System.out.println("  ── Prova de desacoplamento " + "─".repeat(42));
        campo("abstrações (n)", nomes(relatorios));
        campo("implementações (m)", nomes(exportadores));
        campo("combinações", n + " × " + m + " = " + (n * m) + " saídas possíveis");
        campo("herança pura", (n * m) + " subclasses (n × m) — explosão combinatória");
        campo("com Bridge", (n + m) + " classes concretas (n + m)");
        campo("+1 formato (CSV)", "herança +" + n + " classes · Bridge +1 classe, zero edição (OCP)");
        campo("+1 relatório", "herança +" + m + " classes · Bridge +1 classe, zero edição (OCP)");
        System.out.println();
        System.out.println("  ✔ 3 rotinas concluídas · " + GERADOS.size() + " arquivos em " + SAIDA.toAbsolutePath());
        System.out.println();
    }

    // ------------------------------------------------------------ console helpers

    private static void banner() {
        String linha = "═".repeat(66);
        System.out.println();
        System.out.println("  ╔" + linha + "╗");
        System.out.println("  ║  TechFatec BI · Módulo de Relatórios · Padrão Bridge (GoF)       ║");
        System.out.println("  ║  Abstração: O QUE relatar   ↔   Implementação: COMO exportar     ║");
        System.out.println("  ╚" + linha + "╝");
    }

    private static void etapa(int numero, String titulo) {
        System.out.println();
        System.out.println("  ► Rotina " + numero + "/3 — " + titulo);
    }

    private static void campo(String rotulo, String valor) {
        String pontos = ".".repeat(Math.max(2, 22 - rotulo.length()));
        System.out.println("    " + rotulo + " " + pontos + " " + valor);
    }

    private static String id(Object o) {
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    private static String nome(Object o) {
        return o.getClass().getSimpleName();
    }

    private static String nomes(List<? extends Class<?>> classes) {
        return String.join(", ", classes.stream().map(Class::getSimpleName).toList());
    }

    private static String kb(long bytes) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f KB", bytes / 1024.0);
    }

    private static boolean comeca(byte[] dados, byte[] prefixo) {
        return dados.length >= prefixo.length && Arrays.equals(dados, 0, prefixo.length, prefixo, 0, prefixo.length);
    }
}
