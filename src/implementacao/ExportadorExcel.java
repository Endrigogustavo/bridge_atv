package implementacao;

import implementacao.Documento.Bloco;
import implementacao.Documento.Indicador;
import implementacao.Documento.Indicadores;
import implementacao.Documento.Paragrafo;
import implementacao.Documento.Secao;
import implementacao.Documento.Tabela;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * IMPLEMENTADOR CONCRETO: Excel (XLSX / Office Open XML).
 *
 * <p>Monta o pacote .xlsx do zero (um ZIP com as partes XML do SpreadsheetML),
 * sem Apache POI nem qualquer dependência. Estratégia de layout:</p>
 * <ul>
 *   <li>aba <b>Resumo</b>: título, carimbo da ponte, seções, textos e KPIs;</li>
 *   <li>uma aba por {@link Tabela}, com cabeçalho congelado e autofiltro;</li>
 *   <li>números são gravados como números de verdade (somáveis no Excel).</li>
 * </ul>
 */
public final class ExportadorExcel implements Exportador {

    private static final int ESTILO_PADRAO = 0;
    private static final int ESTILO_CABECALHO = 1;
    private static final int ESTILO_DECIMAL = 2;
    private static final int ESTILO_TITULO = 3;

    private record Linha(List<Object> celulas, int estilo, boolean contaLargura) { }

    private record Planilha(String nome, List<Linha> linhas, boolean tabela) { }

    @Override
    public String formato() {
        return "Excel (XLSX)";
    }

    @Override
    public String extensao() {
        return "xlsx";
    }

    @Override
    public byte[] renderizar(Documento documento) {
        List<Planilha> planilhas = new ArrayList<>();
        List<Linha> resumo = new ArrayList<>();
        planilhas.add(new Planilha("Resumo", resumo, false));
        Set<String> nomesUsados = new HashSet<>(Set.of("resumo"));

        resumo.add(new Linha(List.of(documento.titulo()), ESTILO_TITULO, false));
        resumo.add(new Linha(List.of(documento.subtitulo()), ESTILO_PADRAO, false));
        resumo.add(new Linha(List.of(Formatador.carimbo(documento, this)), ESTILO_PADRAO, false));

        for (Bloco bloco : documento.blocos()) {
            switch (bloco) {
                case Secao s -> {
                    resumo.add(new Linha(List.of(), ESTILO_PADRAO, false));
                    resumo.add(new Linha(List.of(s.titulo()), ESTILO_CABECALHO, false));
                }
                case Paragrafo p -> resumo.add(new Linha(List.of(p.texto()), ESTILO_PADRAO, false));
                case Indicadores i -> {
                    for (Indicador k : i.itens()) {
                        resumo.add(new Linha(List.<Object>of(k.rotulo(), k.valor()), ESTILO_PADRAO, true));
                    }
                }
                case Tabela t -> {
                    String nome = nomeUnico(t.titulo(), nomesUsados);
                    resumo.add(new Linha(List.of("→ Tabela completa na aba \"" + nome + "\""), ESTILO_PADRAO, false));
                    List<Linha> linhas = new ArrayList<>();
                    linhas.add(new Linha(new ArrayList<>(t.colunas()), ESTILO_CABECALHO, true));
                    for (List<Object> l : t.linhas()) {
                        linhas.add(new Linha(l, ESTILO_PADRAO, true));
                    }
                    planilhas.add(new Planilha(nome, linhas, true));
                }
            }
        }
        return empacotar(planilhas);
    }

    // ------------------------------------------------------------ pacote OOXML

    private static byte[] empacotar(List<Planilha> planilhas) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            entrada(zip, "[Content_Types].xml", tiposDeConteudo(planilhas.size()));
            entrada(zip, "_rels/.rels", RELS_RAIZ);
            entrada(zip, "xl/workbook.xml", pastaDeTrabalho(planilhas));
            entrada(zip, "xl/_rels/workbook.xml.rels", relsPastaDeTrabalho(planilhas.size()));
            entrada(zip, "xl/styles.xml", ESTILOS);
            for (int i = 0; i < planilhas.size(); i++) {
                entrada(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", folha(planilhas.get(i), i == 0));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao montar o XLSX", e);
        }
        return bytes.toByteArray();
    }

    private static void entrada(ZipOutputStream zip, String caminho, String xml) throws IOException {
        zip.putNextEntry(new ZipEntry(caminho));
        zip.write(xml.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String tiposDeConteudo(int folhas) {
        StringBuilder x = new StringBuilder(XML)
            .append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
            .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
            .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
            .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
            .append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        for (int i = 1; i <= folhas; i++) {
            x.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
             .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return x.append("</Types>").toString();
    }

    private static String pastaDeTrabalho(List<Planilha> planilhas) {
        StringBuilder x = new StringBuilder(XML)
            .append("<workbook xmlns=\"").append(NS_MAIN).append("\" xmlns:r=\"").append(NS_REL).append("\"><sheets>");
        for (int i = 0; i < planilhas.size(); i++) {
            x.append("<sheet name=\"").append(esc(planilhas.get(i).nome())).append("\" sheetId=\"").append(i + 1)
             .append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        x.append("</sheets>");
        StringBuilder filtros = new StringBuilder();
        for (int i = 0; i < planilhas.size(); i++) {
            Planilha p = planilhas.get(i);
            if (p.tabela() && p.linhas().size() > 1) {
                filtros.append("<definedName name=\"_xlnm._FilterDatabase\" localSheetId=\"").append(i).append("\" hidden=\"1\">'")
                       .append(esc(p.nome())).append("'!$A$1:$").append(coluna(colunas(p) - 1)).append('$')
                       .append(p.linhas().size()).append("</definedName>");
            }
        }
        if (!filtros.isEmpty()) {
            x.append("<definedNames>").append(filtros).append("</definedNames>");
        }
        return x.append("</workbook>").toString();
    }

    private static String relsPastaDeTrabalho(int folhas) {
        StringBuilder x = new StringBuilder(XML)
            .append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 1; i <= folhas; i++) {
            x.append("<Relationship Id=\"rId").append(i)
             .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
             .append(i).append(".xml\"/>");
        }
        x.append("<Relationship Id=\"rId").append(folhas + 1)
         .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        return x.append("</Relationships>").toString();
    }

    private static String folha(Planilha p, boolean ativa) {
        int n = colunas(p);
        double[] larguras = new double[n];
        java.util.Arrays.fill(larguras, 12);
        for (Linha l : p.linhas()) {
            if (!l.contaLargura()) continue;
            for (int c = 0; c < l.celulas().size(); c++) {
                double w = Formatador.texto(l.celulas().get(c)).length() * 1.15 + 4;
                larguras[c] = Math.min(55, Math.max(larguras[c], w));
            }
        }

        StringBuilder x = new StringBuilder(XML)
            .append("<worksheet xmlns=\"").append(NS_MAIN).append("\" xmlns:r=\"").append(NS_REL).append("\">")
            .append("<sheetViews><sheetView workbookViewId=\"0\"")
            .append(ativa ? " tabSelected=\"1\"" : "")
            .append(p.tabela() ? ">" : " showGridLines=\"0\">");
        if (p.tabela()) {
            x.append("<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>");
        }
        x.append("</sheetView></sheetViews><cols>");
        for (int c = 0; c < n; c++) {
            x.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
             .append("\" width=\"").append(String.format(java.util.Locale.ROOT, "%.1f", larguras[c]))
             .append("\" customWidth=\"1\"/>");
        }
        x.append("</cols><sheetData>");
        for (int r = 0; r < p.linhas().size(); r++) {
            Linha l = p.linhas().get(r);
            x.append("<row r=\"").append(r + 1).append("\">");
            for (int c = 0; c < l.celulas().size(); c++) {
                x.append(celula(coluna(c) + (r + 1), l.celulas().get(c), l.estilo()));
            }
            x.append("</row>");
        }
        x.append("</sheetData>");
        if (p.tabela() && p.linhas().size() > 1) {
            x.append("<autoFilter ref=\"A1:").append(coluna(n - 1)).append(p.linhas().size()).append("\"/>");
        }
        return x.append("</worksheet>").toString();
    }

    private static String celula(String ref, Object valor, int estiloLinha) {
        if (valor instanceof Number numero) {
            boolean inteiro = numero instanceof Integer || numero instanceof Long;
            return "<c r=\"" + ref + "\" s=\"" + (inteiro ? ESTILO_PADRAO : ESTILO_DECIMAL) + "\"><v>"
                    + numeroXml(numero) + "</v></c>";
        }
        String texto = Formatador.texto(valor);
        if (texto.isEmpty()) {
            return "";
        }
        return "<c r=\"" + ref + "\" s=\"" + estiloLinha + "\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"
                + esc(texto) + "</t></is></c>";
    }

    // --------------------------------------------------------------- utilidades

    private static int colunas(Planilha p) {
        return Math.max(1, p.linhas().stream().mapToInt(l -> l.celulas().size()).max().orElse(1));
    }

    private static String numeroXml(Number n) {
        if (n instanceof BigDecimal b) return b.toPlainString();
        if (n instanceof Integer || n instanceof Long) return n.toString();
        return BigDecimal.valueOf(n.doubleValue()).toPlainString();
    }

    /** Índice 0 → "A", 25 → "Z", 26 → "AA"... */
    private static String coluna(int indice) {
        StringBuilder s = new StringBuilder();
        for (int i = indice + 1; i > 0; i = (i - 1) / 26) {
            s.insert(0, (char) ('A' + (i - 1) % 26));
        }
        return s.toString();
    }

    /** Nome de aba válido no Excel: sem \ / ? * [ ] : ', até 31 caracteres e único. */
    private static String nomeUnico(String titulo, Set<String> usados) {
        String base = titulo.replaceAll("[\\\\/?*\\[\\]:']", " ").trim();
        if (base.isEmpty()) base = "Tabela";
        if (base.length() > 31) base = base.substring(0, 31).trim();
        String nome = base;
        for (int i = 2; usados.contains(nome.toLowerCase()); i++) {
            String sufixo = " (" + i + ")";
            nome = base.substring(0, Math.min(base.length(), 31 - sufixo.length())) + sufixo;
        }
        usados.add(nome.toLowerCase());
        return nome;
    }

    private static String esc(String s) {
        StringBuilder r = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '&' -> r.append("&amp;");
                case '<' -> r.append("&lt;");
                case '>' -> r.append("&gt;");
                case '"' -> r.append("&quot;");
                default -> {
                    if (ch >= 0x20 || ch == '\t' || ch == '\n') r.append(ch);
                }
            }
        }
        return r.toString();
    }

    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    private static final String NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    private static final String RELS_RAIZ = XML
        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
        + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
        + "</Relationships>";

    /** 0 = padrão · 1 = cabeçalho (negrito + fundo) · 2 = decimal #.##0,00 · 3 = título. */
    private static final String ESTILOS = XML
        + "<styleSheet xmlns=\"" + NS_MAIN + "\">"
        + "<fonts count=\"3\">"
        + "<font><sz val=\"11\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
        + "<font><b/><sz val=\"11\"/><color rgb=\"FF1D2230\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
        + "<font><b/><sz val=\"16\"/><color rgb=\"FF0F766E\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
        + "</fonts>"
        + "<fills count=\"3\">"
        + "<fill><patternFill patternType=\"none\"/></fill>"
        + "<fill><patternFill patternType=\"gray125\"/></fill>"
        + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE6F3F1\"/><bgColor indexed=\"64\"/></patternFill></fill>"
        + "</fills>"
        + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
        + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
        + "<cellXfs count=\"4\">"
        + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
        + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>"
        + "<xf numFmtId=\"4\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
        + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
        + "</cellXfs>"
        + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
        + "</styleSheet>";
}
