package br.com.conde.coerencia;

import java.util.List;

/**
 * O JUIZ: uma memoria sem cache nenhum, e a invariante conferida a cada acesso.
 *
 * <p>Ele e o juiz certo e nao so o conveniente porque nao e um protocolo. E um vetor. Os
 * acessos acontecem um depois do outro, na ordem em que estao na carga, e o que uma leitura
 * tem de devolver e o ultimo valor escrito naquela posicao. Nao ha estado, nao ha
 * barramento, nao ha invalidacao.
 *
 * <p>Se o protocolo devolve outro valor em qualquer leitura de qualquer carga, ele esta
 * errado, e nenhuma propriedade interna dele detectaria isso.
 *
 * <p>E o juiz confere TAMBEM a invariante da coerencia depois de cada acesso, nao so no fim.
 * Um protocolo pode acertar todos os valores e passar por estados impossiveis no caminho,
 * e esses estados sao de onde sairiam os erros com qualquer carga um pouco diferente.
 */
public final class Juiz {

    private Juiz() {}

    public record Veredito(String protocolo, String carga, int acessos, int leituras,
            int valoresErrados, int estadosInvalidos, String primeiroProblema) {

        public boolean certo() {
            return valoresErrados == 0 && estadosInvalidos == 0;
        }
    }

    public static Veredito julgar(Protocolo protocolo, Cargas.Carga carga) {
        Maquina maquina = new Maquina(protocolo, Cargas.NUCLEOS, Cargas.LINHAS,
                Cargas.PALAVRAS);

        int[][] serial = new int[Cargas.LINHAS][Cargas.PALAVRAS];

        int leituras = 0;
        int errados = 0;
        int invalidos = 0;

        String primeiro = "";

        for (Acesso acesso : carga.acessos()) {
            if (acesso.escrita()) {
                maquina.escrever(acesso.nucleo(), acesso.linha(), acesso.palavra(),
                        acesso.valor());

                serial[acesso.linha()][acesso.palavra()] = acesso.valor();
            } else {
                leituras++;

                int veio = maquina.ler(acesso.nucleo(), acesso.linha(), acesso.palavra());

                int deveria = serial[acesso.linha()][acesso.palavra()];

                if (veio != deveria) {
                    errados++;

                    if (primeiro.isEmpty())
                        primeiro = acesso + ": veio " + veio + ", devia ser " + deveria;
                }
            }

            List<String> violacoes = maquina.violacoes();

            if (violacoes.isEmpty()) continue;

            invalidos++;

            if (primeiro.isEmpty()) primeiro = acesso + ": " + violacoes.get(0);
        }

        maquina.esvaziar();

        /* No fim, a memoria tem de ter exatamente o que a memoria serial tem. */
        for (int linha = 0; linha < Cargas.LINHAS; linha++)
            for (int palavra = 0; palavra < Cargas.PALAVRAS; palavra++) {
                if (maquina.naMemoria(linha, palavra) == serial[linha][palavra]) continue;

                errados++;

                if (primeiro.isEmpty())
                    primeiro = "no fim, a memoria tem " + maquina.naMemoria(linha, palavra)
                            + " em " + linha + ":" + palavra + " e devia ter "
                            + serial[linha][palavra];
            }

        return new Veredito(protocolo.nome(), carga.nome(), carga.acessos().size(), leituras,
                errados, invalidos, primeiro);
    }

    /** Roda a carga e devolve a maquina, para quem quer contar transacoes. */
    public static Maquina rodar(Protocolo protocolo, Cargas.Carga carga) {
        Maquina maquina = new Maquina(protocolo, Cargas.NUCLEOS, Cargas.LINHAS,
                Cargas.PALAVRAS);

        for (Acesso acesso : carga.acessos())
            if (acesso.escrita())
                maquina.escrever(acesso.nucleo(), acesso.linha(), acesso.palavra(),
                        acesso.valor());
            else maquina.ler(acesso.nucleo(), acesso.linha(), acesso.palavra());

        maquina.esvaziar();

        return maquina;
    }
}
