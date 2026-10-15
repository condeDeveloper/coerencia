package br.com.conde.coerencia;

import java.util.ArrayList;
import java.util.List;

/**
 * AS CARGAS: cada protocolo so e melhor ou pior contra uma delas.
 *
 * <p>Nao existe "qual protocolo e melhor". Existe qual padrao de acesso o programa tem, e a
 * resposta muda por um fator, nao por uma margem. As cargas daqui sao as quatro que
 * aparecem em qualquer programa de varios nucleos, mais a quinta que ninguem escreve de
 * proposito.
 */
public final class Cargas {

    private Cargas() {}

    /** Quantos nucleos as cargas usam. */
    public static final int NUCLEOS = 4;

    /** Quantas palavras cabem numa linha de cache. */
    public static final int PALAVRAS = 8;

    /** Quantas linhas a memoria tem. */
    public static final int LINHAS = 64;

    /** Quantos acessos cada carga gera. */
    public static final int ACESSOS = 4000;

    public record Carga(String nome, List<Acesso> acessos, String oQueEla) {}

    /**
     * PRIVADA: cada nucleo so mexe nas linhas dele, e le antes de escrever.
     *
     * <p>E a carga mais comum que existe, e a que o estado exclusivo ataca: o nucleo le uma
     * posicao que ninguem mais tem e escreve nela em seguida.
     */
    public static Carga privada(Sorteio sorteio) {
        return privadaCom(LINHAS / NUCLEOS, sorteio);
    }

    /** A mesma carga privada com um numero escolhido de linhas por nucleo. */
    public static Carga privadaCom(int linhasPorNucleo, Sorteio sorteio) {
        List<Acesso> saida = new ArrayList<>();

        for (int passo = 0; passo < ACESSOS / 2; passo++) {
            int nucleo = sorteio.ate(NUCLEOS);
            int linha = nucleo * linhasPorNucleo + sorteio.ate(linhasPorNucleo);
            int palavra = sorteio.ate(PALAVRAS);

            saida.add(Acesso.leitura(nucleo, linha, palavra));
            saida.add(Acesso.escrita(nucleo, linha, palavra, passo));
        }

        return new Carga("privada " + linhasPorNucleo, saida,
                "cada nucleo em " + linhasPorNucleo + " linhas so dele, le e escreve");
    }

    /** SO LEITURA: todos leem as mesmas linhas e ninguem escreve. */
    public static Carga soLeitura(Sorteio sorteio) {
        List<Acesso> saida = new ArrayList<>();

        for (int passo = 0; passo < ACESSOS; passo++)
            saida.add(Acesso.leitura(sorteio.ate(NUCLEOS), sorteio.ate(LINHAS),
                    sorteio.ate(PALAVRAS)));

        return new Carga("so leitura", saida, "todos leem as mesmas linhas");
    }

    /**
     * MIGRATORIA: a linha passa de nucleo em nucleo, e cada um le e escreve.
     *
     * <p>E o padrao de um dado protegido por um cadeado que varias linhas de execucao
     * disputam, e e o pior caso de qualquer protocolo de invalidacao.
     */
    public static Carga migratoria(Sorteio sorteio) {
        List<Acesso> saida = new ArrayList<>();

        for (int passo = 0; passo < ACESSOS / 2; passo++) {
            int nucleo = passo % NUCLEOS;
            int linha = sorteio.ate(LINHAS);

            saida.add(Acesso.leitura(nucleo, linha, 0));
            saida.add(Acesso.escrita(nucleo, linha, 0, passo));
        }

        return new Carga("migratoria", saida, "a linha passa de nucleo em nucleo");
    }

    /**
     * PRODUTOR E CONSUMIDOR: um nucleo escreve, os outros leem.
     *
     * <p>E a carga que o estado dono ataca: a linha suja vai do produtor para o consumidor
     * sem que a memoria precise participar.
     */
    public static Carga produtorConsumidor(Sorteio sorteio) {
        return produtorCom(NUCLEOS - 1, sorteio);
    }

    /** O mesmo com um numero escolhido de consumidores. */
    public static Carga produtorCom(int consumidores, Sorteio sorteio) {
        List<Acesso> saida = new ArrayList<>();

        for (int passo = 0; passo < ACESSOS / (consumidores + 1); passo++) {
            int linha = sorteio.ate(LINHAS);

            saida.add(Acesso.escrita(0, linha, 0, passo));

            for (int consumidor = 1; consumidor <= consumidores; consumidor++)
                saida.add(Acesso.leitura(consumidor, linha, 0));
        }

        return new Carga(consumidores == NUCLEOS - 1 ? "produtor e consumidor"
                : "produtor e " + consumidores, saida, "um escreve, " + consumidores
                + " leem");
    }

    /**
     * COMPARTILHAMENTO FALSO: cada nucleo escreve numa palavra DIFERENTE da MESMA linha.
     *
     * <p>Nenhum nucleo toca no dado do outro. Nao ha disputa nenhuma no programa, e o
     * hardware nao sabe disso, porque a coerencia e por linha.
     */
    public static Carga falsa(Sorteio sorteio) {
        return falsaCom(NUCLEOS, sorteio);
    }

    /** O mesmo com um numero escolhido de nucleos brigando pela linha. */
    public static Carga falsaCom(int quantos, Sorteio sorteio) {
        List<Acesso> saida = new ArrayList<>();

        for (int passo = 0; passo < ACESSOS; passo++) {
            int nucleo = sorteio.ate(quantos);

            saida.add(Acesso.escrita(nucleo, 0, nucleo, passo));
        }

        return new Carga(quantos == NUCLEOS ? "compartilhamento falso"
                : "falso com " + quantos, saida,
                "cada nucleo numa palavra diferente da MESMA linha");
    }

    /** O mesmo programa do falso, com o enchimento, para um numero de nucleos. */
    public static Carga comEnchimentoCom(int quantos, Sorteio sorteio) {
        List<Acesso> saida = new ArrayList<>();

        for (int passo = 0; passo < ACESSOS; passo++) {
            int nucleo = sorteio.ate(quantos);

            saida.add(Acesso.escrita(nucleo, nucleo, 0, passo));
        }

        return new Carga(quantos == NUCLEOS ? "com enchimento" : "enchimento com " + quantos,
                saida, "cada nucleo na sua propria linha");
    }

    /**
     * O MESMO PROGRAMA COM ENCHIMENTO: cada nucleo na sua linha.
     *
     * <p>E exatamente a mesma sequencia de escritas, com os dados espalhados para que cada
     * um caia numa linha diferente. E a unica diferenca entre esta carga e a anterior, e a
     * tabela mede o que ela vale.
     */
    public static Carga comEnchimento(Sorteio sorteio) {
        return comEnchimentoCom(NUCLEOS, sorteio);
    }

    private static Carga comNome(Carga carga, String nome) {
        return new Carga(nome, carga.acessos(), carga.oQueEla());
    }

    /** Uma carga qualquer, para o juiz ter o que julgar alem dos padroes escolhidos. */
    public static Carga sorteada(Sorteio sorteio, int quantos) {
        List<Acesso> saida = new ArrayList<>();

        for (int passo = 0; passo < quantos; passo++) {
            int nucleo = sorteio.ate(NUCLEOS);
            int linha = sorteio.ate(LINHAS);
            int palavra = sorteio.ate(PALAVRAS);

            saida.add(sorteio.com(0.4) ? Acesso.escrita(nucleo, linha, palavra, passo)
                    : Acesso.leitura(nucleo, linha, palavra));
        }

        return new Carga("sorteada", saida, "qualquer coisa, com 40% de escrita");
    }

    /** As seis, na ordem das tabelas. */
    public static List<Carga> todas(long semente) {
        return List.of(comNome(privada(new Sorteio(semente)), "privada"),
                soLeitura(new Sorteio(semente)),
                migratoria(new Sorteio(semente)), produtorConsumidor(new Sorteio(semente)),
                falsa(new Sorteio(semente)), comEnchimento(new Sorteio(semente)));
    }
}
