package abstracao;

import implementacao.Documento.Bloco;
import implementacao.Documento.Indicador;
import implementacao.Documento.Indicadores;
import implementacao.Documento.Paragrafo;
import implementacao.Documento.Secao;
import implementacao.Documento.Tabela;
import implementacao.Exportador;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ABSTRAÇÃO REFINADA: Relatório de Vendas (o relatório do sistema legado).
 *
 * <p>Só conhece regras de negócio de vendas: receita, ticket médio,
 * participação por região e ranking de vendedores. Não sabe em que formato
 * será exportado.</p>
 */
public final class RelatorioVendas extends Relatorio {

    /** Uma venda registrada no período. */
    public record Venda(LocalDate data, String regiao, String vendedor, String produto,
                        int quantidade, BigDecimal precoUnitario) {

        public BigDecimal total() {
            return precoUnitario.multiply(BigDecimal.valueOf(quantidade)).setScale(2, RoundingMode.HALF_EVEN);
        }
    }

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String periodo;
    private final List<Venda> vendas;

    /** A implementação (formato) é INJETADA; os dados de negócio também. */
    public RelatorioVendas(Exportador exportador, String periodo, List<Venda> vendas) {
        super(exportador);
        this.periodo = periodo;
        this.vendas = List.copyOf(vendas);
    }

    @Override
    protected String titulo() {
        return "Relatório de Vendas";
    }

    @Override
    protected String subtitulo() {
        return "Período: " + periodo + " · " + vendas.size() + " vendas analisadas";
    }

    @Override
    protected String nomeBase() {
        return "relatorio-vendas-" + slug(periodo);
    }

    @Override
    protected List<Bloco> montarConteudo() {
        BigDecimal receita = somar(vendas);
        int unidades = vendas.stream().mapToInt(Venda::quantidade).sum();
        BigDecimal ticketMedio = vendas.isEmpty() ? BigDecimal.ZERO
                : receita.divide(BigDecimal.valueOf(vendas.size()), 2, RoundingMode.HALF_EVEN);

        // Ranking de regiões por receita
        List<Map.Entry<String, List<Venda>>> regioes = vendas.stream()
                .collect(Collectors.groupingBy(Venda::regiao))
                .entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<String, List<Venda>> e) -> somar(e.getValue())).reversed())
                .toList();

        List<List<Object>> linhasRegiao = new ArrayList<>();
        for (Map.Entry<String, List<Venda>> regiao : regioes) {
            BigDecimal receitaRegiao = somar(regiao.getValue());
            linhasRegiao.add(List.<Object>of(
                    regiao.getKey(),
                    regiao.getValue().size(),
                    regiao.getValue().stream().mapToInt(Venda::quantidade).sum(),
                    receitaRegiao,
                    percentual(fracao(receitaRegiao, receita))));
        }

        // Destaque individual
        Map.Entry<String, BigDecimal> destaque = vendas.stream()
                .collect(Collectors.groupingBy(Venda::vendedor,
                        Collectors.reducing(BigDecimal.ZERO, Venda::total, BigDecimal::add)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(Map.entry("—", BigDecimal.ZERO));

        List<List<Object>> detalhamento = vendas.stream()
                .sorted(Comparator.comparing(Venda::data))
                .map(v -> List.<Object>of(v.data().format(DATA), v.regiao(), v.vendedor(), v.produto(),
                        v.quantidade(), v.precoUnitario(), v.total()))
                .toList();

        String regiaoLider = regioes.isEmpty() ? "—" : regioes.get(0).getKey();
        String participacaoLider = linhasRegiao.isEmpty() ? "0%" : (String) linhasRegiao.get(0).get(4);

        return List.of(
                new Secao("Visão geral"),
                new Indicadores(List.of(
                        new Indicador("Receita total", moeda(receita)),
                        new Indicador("Unidades vendidas", decimal(BigDecimal.valueOf(unidades), 0)),
                        new Indicador("Ticket médio", moeda(ticketMedio)),
                        new Indicador("Região líder", regiaoLider))),
                new Paragrafo("A região " + regiaoLider + " concentrou " + participacaoLider
                        + " da receita do período. O destaque individual foi " + destaque.getKey()
                        + ", com " + moeda(destaque.getValue()) + " em vendas fechadas."),
                new Secao("Desempenho regional"),
                new Tabela("Receita por região",
                        List.of("Região", "Vendas", "Unidades", "Receita (R$)", "Participação"),
                        linhasRegiao),
                new Secao("Transações do período"),
                new Tabela("Detalhamento das vendas",
                        List.of("Data", "Região", "Vendedor", "Produto", "Qtd.", "Preço unit. (R$)", "Total (R$)"),
                        detalhamento));
    }

    private static BigDecimal somar(List<Venda> lista) {
        return lista.stream().map(Venda::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static double fracao(BigDecimal parte, BigDecimal todo) {
        return todo.signum() == 0 ? 0 : parte.divide(todo, 6, RoundingMode.HALF_EVEN).doubleValue();
    }
}
