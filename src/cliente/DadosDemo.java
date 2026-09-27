package cliente;

import abstracao.RelatorioDesempenhoRH.Avaliacao;
import abstracao.RelatorioVendas.Venda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Massa de dados fictícia da TechFatec. Em produção viria de um repositório
 * (banco, API, SAP...) — e também seria injetada nos relatórios, nunca criada por eles.
 */
final class DadosDemo {

    private DadosDemo() { }

    static List<Venda> vendas() {
        return List.of(
            venda(2026, 7, 2, "Sudeste", "Ana Souza", "Licença BI Pro", 12, "1890.00"),
            venda(2026, 7, 5, "Sul", "Bruno Lima", "Dashboard Analytics", 8, "940.00"),
            venda(2026, 7, 9, "Nordeste", "Carla Menezes", "Conector SAP B1", 3, "4200.00"),
            venda(2026, 7, 14, "Sudeste", "Diego Rocha", "Suporte Premium", 20, "310.00"),
            venda(2026, 7, 21, "Centro-Oeste", "Elisa Prado", "Licença BI Pro", 6, "1890.00"),
            venda(2026, 7, 28, "Norte", "Felipe Arruda", "Treinamento Data Lab", 15, "480.00"),
            venda(2026, 8, 4, "Sudeste", "Ana Souza", "Conector SAP B1", 4, "4200.00"),
            venda(2026, 8, 8, "Sul", "Bruno Lima", "Licença BI Pro", 5, "1890.00"),
            venda(2026, 8, 13, "Nordeste", "Carla Menezes", "Dashboard Analytics", 10, "940.00"),
            venda(2026, 8, 19, "Sudeste", "Diego Rocha", "Licença BI Pro", 9, "1890.00"),
            venda(2026, 8, 26, "Centro-Oeste", "Elisa Prado", "Suporte Premium", 14, "310.00"),
            venda(2026, 9, 2, "Sul", "Gabriela Torres", "Conector SAP B1", 2, "4200.00"),
            venda(2026, 9, 9, "Sudeste", "Ana Souza", "Dashboard Analytics", 11, "940.00"),
            venda(2026, 9, 15, "Norte", "Felipe Arruda", "Licença BI Pro", 3, "1890.00"),
            venda(2026, 9, 18, "Nordeste", "Henrique Sales", "Treinamento Data Lab", 18, "480.00"),
            venda(2026, 9, 23, "Sudeste", "Diego Rocha", "Conector SAP B1", 2, "4200.00"));
    }

    static List<Avaliacao> avaliacoes() {
        return List.of(
            new Avaliacao("Mariana Alves", "Tecnologia", "Engenheira de Dados", 9.6, 9.1, 5, 5),
            new Avaliacao("Rafael Nunes", "Tecnologia", "Desenvolvedor Backend", 8.7, 7.9, 4, 5),
            new Avaliacao("Juliana Castro", "Tecnologia", "Analista de QA", 7.2, 8.4, 4, 5),
            new Avaliacao("Pedro Henrique", "Comercial", "Executivo de Contas", 7.8, 8.8, 5, 6),
            new Avaliacao("Larissa Moura", "Comercial", "SDR", 6.1, 7.0, 3, 5),
            new Avaliacao("Thiago Barros", "Financeiro", "Analista Contábil", 8.1, 7.6, 4, 4),
            new Avaliacao("Camila Ribeiro", "Financeiro", "Controller", 9.0, 8.9, 5, 5),
            new Avaliacao("Lucas Ferreira", "Operações", "Analista de Suporte", 5.4, 6.2, 2, 5),
            new Avaliacao("Beatriz Lopes", "Operações", "Coordenadora de Operações", 8.3, 9.2, 5, 6),
            new Avaliacao("Gustavo Pires", "Pessoas & Cultura", "Business Partner", 7.9, 8.6, 4, 5));
    }

    private static Venda venda(int ano, int mes, int dia, String regiao, String vendedor,
                               String produto, int quantidade, String preco) {
        return new Venda(LocalDate.of(ano, mes, dia), regiao, vendedor, produto, quantidade, new BigDecimal(preco));
    }
}
