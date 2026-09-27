package implementacao;

import implementacao.Documento.Bloco;
import implementacao.Documento.Indicador;
import implementacao.Documento.Indicadores;
import implementacao.Documento.Paragrafo;
import implementacao.Documento.Secao;
import implementacao.Documento.Tabela;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * IMPLEMENTADOR CONCRETO: HTML5.
 *
 * <p>Gera uma página única e autocontida (CSS embutido, sem dependências
 * externas), responsiva e com tema claro/escuro automático.</p>
 */
public final class ExportadorHtml implements Exportador {

    @Override
    public String formato() {
        return "HTML";
    }

    @Override
    public String extensao() {
        return "html";
    }

    @Override
    public byte[] renderizar(Documento documento) {
        StringBuilder h = new StringBuilder(16_384);
        h.append("<!DOCTYPE html>\n<html lang=\"pt-BR\">\n<head>\n")
         .append("<meta charset=\"UTF-8\">\n")
         .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
         .append("<title>").append(esc(documento.titulo())).append("</title>\n")
         .append("<style>").append(CSS).append("</style>\n</head>\n<body>\n<main>\n");

        h.append("<header>\n")
         .append("<p class=\"selo\">TechFatec BI · Módulo de Relatórios</p>\n")
         .append("<h1>").append(esc(documento.titulo())).append("</h1>\n")
         .append("<p class=\"sub\">").append(esc(documento.subtitulo())).append("</p>\n")
         .append("<p class=\"ponte\"><span>Abstração <b>").append(esc(documento.origem())).append("</b></span>")
         .append("<span class=\"elo\" aria-hidden=\"true\">⟷</span>")
         .append("<span>Implementação <b>").append(getClass().getSimpleName()).append("</b></span></p>\n")
         .append("</header>\n");

        for (Bloco bloco : documento.blocos()) {
            switch (bloco) {
                case Secao s -> h.append("<h2>").append(esc(s.titulo())).append("</h2>\n");
                case Paragrafo p -> h.append("<p>").append(esc(p.texto())).append("</p>\n");
                case Indicadores i -> indicadores(h, i.itens());
                case Tabela t -> tabela(h, t);
            }
        }

        h.append("<footer>").append(esc(Formatador.carimbo(documento, this)))
         .append(" · Padrão Bridge (GoF)</footer>\n</main>\n</body>\n</html>\n");
        return h.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void indicadores(StringBuilder h, List<Indicador> itens) {
        h.append("<div class=\"kpis\">\n");
        for (Indicador k : itens) {
            h.append("<div class=\"kpi\"><span>").append(esc(k.rotulo())).append("</span><strong>")
             .append(esc(k.valor())).append("</strong></div>\n");
        }
        h.append("</div>\n");
    }

    private static void tabela(StringBuilder h, Tabela t) {
        int colunas = t.colunas().size();
        boolean[] numerica = new boolean[colunas];
        if (!t.linhas().isEmpty()) {
            for (int c = 0; c < colunas; c++) {
                numerica[c] = Formatador.numerico(t.linhas().get(0).get(c));
            }
        }
        h.append("<figure>\n<figcaption>").append(esc(t.titulo())).append("</figcaption>\n")
         .append("<div class=\"rolagem\"><table>\n<thead><tr>");
        for (int c = 0; c < colunas; c++) {
            h.append(numerica[c] ? "<th class=\"num\">" : "<th>").append(esc(t.colunas().get(c))).append("</th>");
        }
        h.append("</tr></thead>\n<tbody>\n");
        for (List<Object> linha : t.linhas()) {
            h.append("<tr>");
            for (int c = 0; c < colunas; c++) {
                Object v = c < linha.size() ? linha.get(c) : "";
                h.append(Formatador.numerico(v) ? "<td class=\"num\">" : "<td>")
                 .append(esc(Formatador.texto(v))).append("</td>");
            }
            h.append("</tr>\n");
        }
        h.append("</tbody>\n</table></div>\n</figure>\n");
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final String CSS = """
        :root{--bg:#f6f5f1;--card:#fff;--ink:#1d2230;--muted:#6b7080;--line:#e4e2da;--accent:#0f766e;--soft:#e6f3f1;--zebra:#faf9f6}
        @media (prefers-color-scheme:dark){:root{--bg:#12151c;--card:#1a1f29;--ink:#e8eaf0;--muted:#9aa0ad;--line:#2a303c;--accent:#5eead4;--soft:#16302d;--zebra:#1e2430}}
        *{box-sizing:border-box}
        body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.6 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
        main{max-width:1040px;margin:0 auto;padding:40px 24px 56px}
        header{border-bottom:2px solid var(--ink);padding-bottom:22px;margin-bottom:8px}
        .selo{font:600 12px/1 ui-monospace,Menlo,Consolas,monospace;letter-spacing:.08em;text-transform:uppercase;color:var(--accent);margin:0 0 14px}
        h1{font-size:clamp(28px,4vw,40px);line-height:1.1;margin:0 0 8px;letter-spacing:-.02em}
        .sub{color:var(--muted);margin:0 0 16px}
        .ponte{display:inline-flex;flex-wrap:wrap;gap:10px;align-items:center;font:13px ui-monospace,Menlo,Consolas,monospace;background:var(--soft);border-radius:999px;padding:6px 16px;margin:0}
        .ponte b{color:var(--accent)}
        .elo{color:var(--accent);font-size:16px}
        h2{font-size:18px;margin:36px 0 14px;display:flex;align-items:center;gap:10px}
        h2::before{content:"";width:10px;height:10px;background:var(--accent);border-radius:2px}
        .kpis{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:12px;margin-bottom:16px}
        .kpi{background:var(--card);border:1px solid var(--line);border-left:4px solid var(--accent);border-radius:10px;padding:14px 18px}
        .kpi span{display:block;font-size:12px;color:var(--muted);text-transform:uppercase;letter-spacing:.05em}
        .kpi strong{display:block;font-size:22px;margin-top:4px;font-variant-numeric:tabular-nums}
        figure{margin:0 0 10px;background:var(--card);border:1px solid var(--line);border-radius:12px;overflow:hidden}
        figcaption{font-weight:600;padding:12px 16px;border-bottom:1px solid var(--line)}
        .rolagem{overflow-x:auto}
        table{border-collapse:collapse;width:100%;font-size:14px}
        th,td{padding:9px 16px;text-align:left;white-space:nowrap}
        th{font-size:12px;text-transform:uppercase;letter-spacing:.04em;color:var(--muted);border-bottom:1px solid var(--line)}
        tbody tr:nth-child(even){background:var(--zebra)}
        .num{text-align:right;font-variant-numeric:tabular-nums}
        footer{margin-top:40px;padding-top:16px;border-top:1px solid var(--line);color:var(--muted);font-size:13px}
        @media print{body{background:#fff}main{padding:0}}
        """;
}
