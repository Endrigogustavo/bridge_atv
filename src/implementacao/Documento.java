package implementacao;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Documento canônico: o "tabuleiro" que atravessa a ponte.
 *
 * <p>A abstração (relatórios) descreve O QUE deve ser mostrado usando apenas
 * estes blocos semânticos; a implementação (exportadores) decide COMO
 * desenhá-los. Nenhum bloco carrega detalhe de formato (fonte, célula, tag).</p>
 *
 * <p>{@link Bloco} é uma interface <b>sealed</b> (Java 17+): o compilador conhece
 * todos os tipos de bloco. Assim, o {@code switch} de cada exportador é
 * verificado em tempo de compilação: se um novo tipo de bloco for criado, todo
 * exportador que ainda não sabe desenhá-lo deixa de compilar, em vez de falhar
 * silenciosamente em produção.</p>
 */
public record Documento(String titulo,
                        String subtitulo,
                        String origem,
                        LocalDateTime geradoEm,
                        List<Bloco> blocos) {

    public Documento {
        blocos = List.copyOf(blocos);
    }

    /** Vocabulário fechado de conteúdo que um relatório pode usar. */
    public sealed interface Bloco permits Secao, Paragrafo, Indicadores, Tabela { }

    /** Título de uma seção do relatório. */
    public record Secao(String titulo) implements Bloco { }

    /** Texto corrido. */
    public record Paragrafo(String texto) implements Bloco { }

    /** Conjunto de KPIs (rótulo + valor já formatado). */
    public record Indicadores(List<Indicador> itens) implements Bloco {
        public Indicadores {
            itens = List.copyOf(itens);
        }
    }

    public record Indicador(String rotulo, String valor) { }

    /**
     * Tabela de dados. As células podem ser {@link Number} ou texto: cada
     * exportador decide como representá-las (o Excel grava números reais,
     * o PDF e o HTML exibem no padrão pt-BR).
     */
    public record Tabela(String titulo, List<String> colunas, List<List<Object>> linhas) implements Bloco {
        public Tabela {
            colunas = List.copyOf(colunas);
            linhas = linhas.stream().map(List::copyOf).toList();
        }
    }
}
