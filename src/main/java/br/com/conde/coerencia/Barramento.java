package br.com.conde.coerencia;

/**
 * O BARRAMENTO: a unidade de medida deste repositorio.
 *
 * <p>Nada aqui e medido em segundos. O que custa num sistema de varios nucleos e a
 * TRANSACAO: um pedido que sai do cache, atravessa o barramento e e visto por todos os
 * outros. Comparado com um acerto local, isso e caro por um fator de dezenas.
 *
 * <p>Sao quatro tipos, e cada um existe por uma razao diferente:
 *
 * <ul>
 *   <li>LEITURA: quero a linha para ler.
 *   <li>LEITURA EXCLUSIVA: quero a linha para escrever, e os outros que joguem fora.
 *   <li>PROMOCAO: ja tenho a linha, quero permissao para escrever. Nao carrega dado nenhum,
 *       e e a transacao que o estado exclusivo existe para evitar.
 *   <li>ESCRITA DE VOLTA: a memoria esta velha e precisa da versao boa.
 * </ul>
 */
public final class Barramento {

    private long leituras;
    private long leiturasExclusivas;
    private long promocoes;
    private long escritasDeVolta;

    private long transferenciasEntreCaches;

    public void leitura() {
        leituras++;
    }

    public void leituraExclusiva() {
        leiturasExclusivas++;
    }

    public void promocao() {
        promocoes++;
    }

    public void escritaDeVolta() {
        escritasDeVolta++;
    }

    public void transferenciaEntreCaches() {
        transferenciasEntreCaches++;
    }

    public long leituras() {
        return leituras;
    }

    public long leiturasExclusivas() {
        return leiturasExclusivas;
    }

    public long promocoes() {
        return promocoes;
    }

    public long escritasDeVolta() {
        return escritasDeVolta;
    }

    public long transferenciasEntreCaches() {
        return transferenciasEntreCaches;
    }

    /** O total que atravessa o barramento. */
    public long total() {
        return leituras + leiturasExclusivas + promocoes + escritasDeVolta;
    }
}
