package implementacao;

import implementacao.Documento.Bloco;
import implementacao.Documento.Indicador;
import implementacao.Documento.Indicadores;
import implementacao.Documento.Paragrafo;
import implementacao.Documento.Secao;
import implementacao.Documento.Tabela;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * IMPLEMENTADOR CONCRETO: PDF 1.4.
 *
 * <p>Escreve o arquivo PDF "na mão", byte a byte (objetos, fluxos de conteúdo,
 * tabela xref e trailer), sem nenhuma biblioteca externa. Usa as fontes padrão
 * Helvetica com codificação WinAnsi, que cobre a acentuação do português.
 * Suporta quebra automática de página e repetição do cabeçalho das tabelas.</p>
 */
public final class ExportadorPdf implements Exportador {

    private static final Charset WIN_ANSI = Charset.forName("windows-1252");

    private static final float LARGURA = 595f;   // A4 em pontos
    private static final float ALTURA = 842f;
    private static final float MARGEM = 50f;
    private static final float UTIL = LARGURA - 2 * MARGEM;
    private static final float NEGRITO = 1.08f;       // Helvetica-Bold é ~8% mais larga
    private static final float COLUNA_CURTA = 75f;    // colunas até 75 pt não são comprimidas

    private static final String TINTA = "0.11 0.13 0.19 rg";
    private static final String SUAVE = "0.42 0.44 0.50 rg";
    private static final String DESTAQUE = "0.06 0.46 0.43 rg";
    private static final String FUNDO_KPI = "0.95 0.95 0.93 rg";
    private static final String FUNDO_CABECALHO = "0.90 0.95 0.94 rg";
    private static final String ZEBRA = "0.975 0.975 0.965 rg";
    private static final String TRACO = "0.80 0.80 0.78 RG";
    private static final String TRACO_FORTE = "0.11 0.13 0.19 RG";

    @Override
    public String formato() {
        return "PDF";
    }

    @Override
    public String extensao() {
        return "pdf";
    }

    @Override
    public byte[] renderizar(Documento documento) {
        Diagramador g = new Diagramador();   // estado local à chamada: exportador sem estado

        g.escrever("F2", 8, MARGEM, "TECHFATEC BI · MÓDULO DE RELATÓRIOS", DESTAQUE);
        g.avancar(4);
        g.escrever("F2", 22, MARGEM, documento.titulo(), TINTA);
        g.escrever("F1", 11, MARGEM, documento.subtitulo(), SUAVE);
        g.avancar(4);
        g.linha(MARGEM, MARGEM + UTIL, g.y, TRACO_FORTE, 1.4f);
        g.avancar(6);

        for (Bloco bloco : documento.blocos()) {
            switch (bloco) {
                case Secao s -> secao(g, s);
                case Paragrafo p -> paragrafo(g, p);
                case Indicadores i -> indicadores(g, i.itens());
                case Tabela t -> tabela(g, t);
            }
        }

        g.rodapes(Formatador.carimbo(documento, this));
        return montarArquivo(g.paginas);
    }

    // ------------------------------------------------------------------ blocos

    private static void secao(Diagramador g, Secao s) {
        g.garantir(60);
        g.avancar(16);
        float base = g.y - 13;
        g.retangulo(MARGEM, base + 1, 7, 7, DESTAQUE);
        g.texto("F2", 13, MARGEM + 14, base, s.titulo(), TINTA);
        g.y = base - 10;
    }

    private static void paragrafo(Diagramador g, Paragrafo p) {
        for (String linha : quebrar(p.texto(), 10, UTIL)) {
            g.garantir(16);
            g.escrever("F1", 10, MARGEM, linha, TINTA);
        }
        g.avancar(6);
    }

    private static void indicadores(Diagramador g, List<Indicador> itens) {
        int porLinha = Math.max(1, Math.min(4, itens.size()));
        float espaco = 8, altura = 46;
        float largura = (UTIL - espaco * (porLinha - 1)) / porLinha;
        for (int inicio = 0; inicio < itens.size(); inicio += porLinha) {
            g.garantir(altura + 12);
            float topo = g.y - 4;
            for (int j = 0; j < porLinha && inicio + j < itens.size(); j++) {
                Indicador k = itens.get(inicio + j);
                float x = MARGEM + j * (largura + espaco);
                g.retangulo(x, topo - altura, largura, altura, FUNDO_KPI);
                g.retangulo(x, topo - altura, 3, altura, DESTAQUE);
                g.texto("F1", 7.5f, x + 11, topo - 15,
                        caber(k.rotulo().toUpperCase(Formatador.PT_BR), 7.5f, largura - 18, 1f), SUAVE);
                g.texto("F2", 13, x + 11, topo - 35, caber(k.valor(), 13, largura - 18, NEGRITO), TINTA);
            }
            g.y = topo - altura - 8;
        }
    }

    private static void tabela(Diagramador g, Tabela t) {
        float[] larguras = larguras(t);
        boolean[] numerica = new boolean[t.colunas().size()];
        if (!t.linhas().isEmpty()) {
            for (int c = 0; c < numerica.length; c++) {
                numerica[c] = Formatador.numerico(t.linhas().get(0).get(c));
            }
        }
        g.garantir(70);
        g.escrever("F2", 9.5f, MARGEM, t.titulo(), SUAVE);
        g.avancar(3);
        cabecalho(g, t, larguras, numerica);

        int indice = 0;
        for (List<Object> linha : t.linhas()) {
            if (g.garantir(16)) {
                cabecalho(g, t, larguras, numerica);   // repete o cabeçalho na nova página
            }
            if (indice++ % 2 == 1) {
                g.retangulo(MARGEM, g.y - 15, UTIL, 15, ZEBRA);
            }
            float x = MARGEM, base = g.y - 10.5f;
            for (int c = 0; c < larguras.length; c++) {
                Object valor = c < linha.size() ? linha.get(c) : "";
                String texto = caber(Formatador.texto(valor), 8, larguras[c] - 10, 1f);
                float tx = numerica[c] ? x + larguras[c] - 5 - largura(texto, 8) : x + 5;
                g.texto("F1", 8, tx, base, texto, TINTA);
                x += larguras[c];
            }
            g.y -= 15;
        }
        g.linha(MARGEM, MARGEM + UTIL, g.y, TRACO, 0.6f);
        g.avancar(12);
    }

    private static void cabecalho(Diagramador g, Tabela t, float[] larguras, boolean[] numerica) {
        g.retangulo(MARGEM, g.y - 17, UTIL, 17, FUNDO_CABECALHO);
        float x = MARGEM, base = g.y - 11.8f;
        for (int c = 0; c < larguras.length; c++) {
            String texto = caber(t.colunas().get(c), 8, larguras[c] - 10, NEGRITO);
            float tx = numerica[c] ? x + larguras[c] - 5 - largura(texto, 8) * NEGRITO : x + 5;
            g.texto("F2", 8, tx, base, texto, TINTA);
            x += larguras[c];
        }
        g.y -= 17;
        g.linha(MARGEM, MARGEM + UTIL, g.y, TRACO_FORTE, 0.8f);
    }

    /**
     * Largura de cada coluna: colunas curtas (números, datas, siglas) recebem
     * exatamente o que precisam; o espaço restante é dividido entre as colunas
     * de texto longo, proporcionalmente ao conteúdo.
     */
    private static float[] larguras(Tabela t) {
        int n = t.colunas().size();
        float[] necessario = new float[n];
        float total = 0;
        for (int c = 0; c < n; c++) {
            float maior = largura(t.colunas().get(c), 8) * NEGRITO;
            for (List<Object> linha : t.linhas()) {
                maior = Math.max(maior, largura(Formatador.texto(linha.get(c)), 8));
            }
            necessario[c] = maior + 12;
            total += necessario[c];
        }
        float[] larguras = new float[n];
        if (total <= UTIL) {
            for (int c = 0; c < n; c++) larguras[c] = necessario[c] + (UTIL - total) * necessario[c] / total;
            return larguras;
        }
        float fixo = 0, flexivel = 0;
        for (float w : necessario) {
            if (w <= COLUNA_CURTA) fixo += w; else flexivel += w;
        }
        float sobra = UTIL - fixo;
        for (int c = 0; c < n; c++) {
            larguras[c] = sobra <= 0 ? UTIL * necessario[c] / total
                    : necessario[c] <= COLUNA_CURTA ? necessario[c] : sobra * necessario[c] / flexivel;
        }
        return larguras;
    }

    // ------------------------------------------------------------- tipografia

    /** Largura aproximada de um texto em Helvetica (usada para alinhar números à direita). */
    private static float largura(String s, float tamanho) {
        float em = 0;
        for (char ch : s.toCharArray()) {
            if (Character.isDigit(ch)) em += 0.556f;
            else if (ch == '.' || ch == ',' || ch == ' ' || ch == '/') em += 0.278f;
            else if (ch == '%') em += 0.889f;
            else if (Character.isUpperCase(ch)) em += 0.667f;
            else em += 0.5f;
        }
        return em * tamanho;
    }

    /** Corta o texto (com reticências) até caber na largura disponível. */
    private static String caber(String s, float tamanho, float disponivel, float fator) {
        if (largura(s, tamanho) * fator <= disponivel) return s;
        String r = s;
        while (r.length() > 1 && largura(r + "…", tamanho) * fator > disponivel) r = r.substring(0, r.length() - 1);
        return r.stripTrailing() + "…";
    }

    /** Quebra o texto em linhas que cabem na largura disponível. */
    private static List<String> quebrar(String texto, float tamanho, float disponivel) {
        List<String> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.split("\\s+")) {
            if (atual.length() > 0 && largura(atual + " " + palavra, tamanho) > disponivel) {
                linhas.add(atual.toString());
                atual.setLength(0);
            }
            if (atual.length() > 0) atual.append(' ');
            atual.append(palavra);
        }
        if (atual.length() > 0) linhas.add(atual.toString());
        return linhas;
    }

    private static String escapar(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replace('\n', ' ').replace('\r', ' ');
    }

    private static String n(float v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    // ------------------------------------------------------ diagramação/páginas

    /** Cursor de diagramação: acumula os operadores de desenho de cada página. */
    private static final class Diagramador {
        final List<StringBuilder> paginas = new ArrayList<>();
        StringBuilder atual;
        float y;

        Diagramador() {
            novaPagina();
        }

        void novaPagina() {
            atual = new StringBuilder(4096);
            paginas.add(atual);
            y = ALTURA - MARGEM;
        }

        /** Garante espaço vertical; se faltar, abre nova página e devolve true. */
        boolean garantir(float altura) {
            if (y - altura < MARGEM + 24) {
                novaPagina();
                return true;
            }
            return false;
        }

        void avancar(float dy) {
            y -= dy;
        }

        void escrever(String fonte, float tamanho, float x, String s, String cor) {
            y -= tamanho;
            texto(fonte, tamanho, x, y, s, cor);
            y -= tamanho * 0.5f;
        }

        void texto(String fonte, float tamanho, float x, float yy, String s, String cor) {
            atual.append(cor).append(" BT /").append(fonte).append(' ').append(n(tamanho)).append(" Tf ")
                 .append(n(x)).append(' ').append(n(yy)).append(" Td (").append(escapar(s)).append(") Tj ET\n");
        }

        void retangulo(float x, float yy, float w, float h, String cor) {
            atual.append(cor).append(' ').append(n(x)).append(' ').append(n(yy)).append(' ')
                 .append(n(w)).append(' ').append(n(h)).append(" re f\n");
        }

        void linha(float x1, float x2, float yy, String cor, float espessura) {
            atual.append(cor).append(' ').append(n(espessura)).append(" w ").append(n(x1)).append(' ').append(n(yy))
                 .append(" m ").append(n(x2)).append(' ').append(n(yy)).append(" l S\n");
        }

        void rodapes(String carimbo) {
            int total = paginas.size();
            for (int i = 0; i < total; i++) {
                atual = paginas.get(i);
                linha(MARGEM, MARGEM + UTIL, 40, TRACO, 0.5f);
                texto("F1", 7, MARGEM, 28, carimbo, SUAVE);
                String numero = "Página " + (i + 1) + " de " + total;
                texto("F1", 7, MARGEM + UTIL - largura(numero, 7), 28, numero, SUAVE);
            }
        }
    }

    // ------------------------------------------------------- estrutura do PDF

    private static byte[] montarArquivo(List<StringBuilder> paginas) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int total = 4 + paginas.size() * 2;
        long[] offsets = new long[total + 1];

        ascii(out, "%PDF-1.4\n");
        out.writeBytes(new byte[] {'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < paginas.size(); i++) {
            kids.append(i == 0 ? "" : " ").append(5 + 2 * i).append(" 0 R");
        }
        objeto(out, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        objeto(out, offsets, 2, "<< /Type /Pages /Kids [" + kids + "] /Count " + paginas.size() + " >>");
        objeto(out, offsets, 3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        objeto(out, offsets, 4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");

        for (int i = 0; i < paginas.size(); i++) {
            int pagina = 5 + 2 * i, conteudo = pagina + 1;
            objeto(out, offsets, pagina, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + conteudo + " 0 R >>");
            byte[] fluxo = paginas.get(i).toString().getBytes(WIN_ANSI);
            offsets[conteudo] = out.size();
            ascii(out, conteudo + " 0 obj\n<< /Length " + fluxo.length + " >>\nstream\n");
            out.writeBytes(fluxo);
            ascii(out, "\nendstream\nendobj\n");
        }

        long xref = out.size();
        StringBuilder x = new StringBuilder("xref\n0 " + (total + 1) + "\n0000000000 65535 f \n");
        for (int i = 1; i <= total; i++) {
            x.append(String.format(Locale.ROOT, "%010d 00000 n \n", offsets[i]));
        }
        x.append("trailer\n<< /Size ").append(total + 1).append(" /Root 1 0 R >>\nstartxref\n")
         .append(xref).append("\n%%EOF\n");
        ascii(out, x.toString());
        return out.toByteArray();
    }

    private static void objeto(ByteArrayOutputStream out, long[] offsets, int numero, String corpo) {
        offsets[numero] = out.size();
        ascii(out, numero + " 0 obj\n" + corpo + "\nendobj\n");
    }

    private static void ascii(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
    }
}
