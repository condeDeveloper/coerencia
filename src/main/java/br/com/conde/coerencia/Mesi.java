package br.com.conde.coerencia;

/**
 * MESI: o MSI com o estado EXCLUSIVO.
 *
 * <p>Na leitura, o barramento responde se mais alguem tem a linha. Se ninguem tem, o cache
 * fica em EXCLUSIVO em vez de COMPARTILHADO, e com isso a escrita seguinte vira um negocio
 * interno: nenhuma transacao.
 *
 * <p>O padrao que isso ataca e o mais comum que existe: ler uma posicao e escrever nela
 * logo depois, num dado que so aquele nucleo usa. A tabela do exclusivo mede quanto disso
 * sobra numa carga de verdade.
 */
public final class Mesi implements Protocolo {

    @Override
    public String nome() {
        return "MESI";
    }

    @Override
    public boolean temExclusivo() {
        return true;
    }

    @Override
    public boolean temDono() {
        return false;
    }
}
