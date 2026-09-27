package abstracao;

import implementacao.Documento.Bloco;
import implementacao.Documento.Indicador;
import implementacao.Documento.Indicadores;
import implementacao.Documento.Paragrafo;
import implementacao.Documento.Secao;
import implementacao.Documento.Tabela;
import implementacao.Exportador;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * ABSTRAÇÃO REFINADA: Relatório de Desempenho de RH (o novo requisito).
 *
 * <p>Entrou no sistema como UMA classe nova. Nenhum exportador foi alterado
 * para suportá-lo: ele já nasce exportável para PDF, Excel e HTML — essa é a
 * prova prática do Princípio Aberto/Fechado.</p>
 */
public final class RelatorioDesempenhoRH extends Relatorio {

    private static final double PESO_TECNICO = 0.6;
    private static final double PESO_COMPORTAMENTAL = 0.4;

    /** Avaliação de um colaborador no ciclo. Notas de 0 a 10. */
    public record Avaliacao(String colaborador, String departamento, String cargo,
                            double notaTecnica, double notaComportamental,
                            int metasAtingidas, int metasPlanejadas) {

        public double notaFinal() {
            return notaTecnica * PESO_TECNICO + notaComportamental * PESO_COMPORTAMENTAL;
        }

        public String classificacao() {
            double nota = notaFinal();
            if (nota >= 9.0) return "Excepcional";
            if (nota >= 7.5) return "Acima do esperado";
            if (nota >= 6.0) return "Dentro do esperado";
            return "Plano de desenvolvimento";
        }
    }

    private final String ciclo;
    private final List<Avaliacao> avaliacoes;

    public RelatorioDesempenhoRH(Exportador exportador, String ciclo, List<Avaliacao> avaliacoes) {
        super(exportador);
        this.ciclo = ciclo;
        this.avaliacoes = List.copyOf(avaliacoes);
    }

    @Override
    protected String titulo() {
        return "Relatório de Desempenho de RH";
    }

    @Override
    protected String subtitulo() {
        return "Ciclo de avaliação: " + ciclo + " · " + avaliacoes.size() + " colaboradores";
    }

    @Override
    protected String nomeBase() {
        return "relatorio-desempenho-rh-" + slug(ciclo);
    }

    @Override
    protected List<Bloco> montarConteudo() {
        double media = avaliacoes.stream().mapToDouble(Avaliacao::notaFinal).average().orElse(0);
        int atingidas = avaliacoes.stream().mapToInt(Avaliacao::metasAtingidas).sum();
        int planejadas = avaliacoes.stream().mapToInt(Avaliacao::metasPlanejadas).sum();
        Avaliacao destaque = avaliacoes.stream().max(Comparator.comparingDouble(Avaliacao::notaFinal)).orElse(null);
        long emDesenvolvimento = avaliacoes.stream().filter(a -> a.notaFinal() < 6.0).count();

        // Consolidado por departamento (ordenado pela média, da maior para a menor)
        Map<String, List<Avaliacao>> porDepartamento = avaliacoes.stream()
                .collect(Collectors.groupingBy(Avaliacao::departamento, TreeMap::new, Collectors.toList()));
        List<List<Object>> linhasDepartamento = new ArrayList<>();
        porDepartamento.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<String, List<Avaliacao>> e) -> mediaFinal(e.getValue())).reversed())
                .forEach(e -> {
                    int at = e.getValue().stream().mapToInt(Avaliacao::metasAtingidas).sum();
                    int pl = e.getValue().stream().mapToInt(Avaliacao::metasPlanejadas).sum();
                    linhasDepartamento.add(List.<Object>of(
                            e.getKey(), e.getValue().size(), arredondar(mediaFinal(e.getValue()), 2),
                            percentual(pl == 0 ? 0 : (double) at / pl)));
                });

        List<List<Object>> individuais = avaliacoes.stream()
                .sorted(Comparator.comparingDouble(Avaliacao::notaFinal).reversed())
                .map(a -> List.<Object>of(a.colaborador(), a.departamento(), a.cargo(),
                        arredondar(a.notaTecnica(), 1), arredondar(a.notaComportamental(), 1),
                        arredondar(a.notaFinal(), 2), a.metasAtingidas() + "/" + a.metasPlanejadas(),
                        a.classificacao()))
                .toList();

        return List.of(
                new Secao("Visão geral"),
                new Indicadores(List.of(
                        new Indicador("Colaboradores avaliados", String.valueOf(avaliacoes.size())),
                        new Indicador("Nota média geral", decimal(BigDecimal.valueOf(media), 2)),
                        new Indicador("Metas atingidas", percentual(planejadas == 0 ? 0 : (double) atingidas / planejadas)),
                        new Indicador("Destaque do ciclo", destaque == null ? "—" : destaque.colaborador()))),
                new Paragrafo("A nota final pondera competências técnicas (60%) e comportamentais (40%). "
                        + "Faixas: Excepcional a partir de 9,0; Acima do esperado a partir de 7,5; "
                        + "Dentro do esperado a partir de 6,0; abaixo disso, Plano de desenvolvimento."),
                new Secao("Consolidado por departamento"),
                new Tabela("Desempenho por departamento",
                        List.of("Departamento", "Colaboradores", "Nota média", "Metas atingidas"),
                        linhasDepartamento),
                new Secao("Resultados individuais"),
                new Tabela("Avaliações individuais",
                        List.of("Colaborador", "Departamento", "Cargo", "Técnica", "Comport.", "Final", "Metas", "Classificação"),
                        individuais),
                new Paragrafo(emDesenvolvimento == 0
                        ? "Nenhum colaborador ficou abaixo da faixa mínima neste ciclo."
                        : emDesenvolvimento + " colaborador(es) receberão Plano de Desenvolvimento Individual (PDI) "
                          + "com acompanhamento mensal da liderança."));
    }

    private static double mediaFinal(List<Avaliacao> lista) {
        return lista.stream().mapToDouble(Avaliacao::notaFinal).average().orElse(0);
    }
}
