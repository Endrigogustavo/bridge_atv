package abstracao;

/**
 * Resultado de {@link Relatorio#gerar()}: o arquivo pronto, independente do formato.
 *
 * @param nome     nome sugerido do arquivo (ex.: relatorio-vendas-3-trimestre-2026.pdf)
 * @param formato  formato legível informado pelo exportador (ex.: "PDF")
 * @param conteudo bytes do arquivo
 */
public record ArquivoExportado(String nome, String formato, byte[] conteudo) {

    public long tamanho() {
        return conteudo.length;
    }
}
