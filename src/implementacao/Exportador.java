package implementacao;

/**
 * IMPLEMENTADOR (Implementor) do padrão Bridge.
 *
 * <p>Define o lado "COMO exportar" da ponte. Cada formato de saída é uma
 * implementação independente desta interface, e nenhuma delas conhece os
 * relatórios concretos: todas trabalham apenas sobre o {@link Documento}
 * canônico, que é neutro de formato.</p>
 *
 * <p>Novo formato (CSV, JSON, DOCX...) = nova classe que implementa esta
 * interface. Nenhuma linha de código existente precisa ser alterada (OCP).</p>
 */
public interface Exportador {

    /** Nome legível do formato, ex.: "PDF". */
    String formato();

    /** Extensão do arquivo gerado, sem ponto, ex.: "pdf". */
    String extensao();

    /**
     * Operação primitiva da ponte: transforma o documento neutro nos bytes
     * do formato de destino. Implementações não guardam estado entre
     * chamadas, por isso a mesma instância pode ser reutilizada e trocada
     * livremente em tempo de execução.
     */
    byte[] renderizar(Documento documento);
}
