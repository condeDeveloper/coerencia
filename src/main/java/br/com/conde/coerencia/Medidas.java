package br.com.conde.coerencia;

import java.util.ArrayList;
import java.util.List;

/**
 * AS MEDIDAS: tudo o que o repositorio afirma, calculado aqui e em nenhum outro lugar.
 *
 * <p>O Medidor imprime o que sai daqui e o teste confere o que sai daqui. Os dois leem a
 * MESMA funcao, entao nao existe a situacao de o texto dizer uma coisa e o teste conferir
 * outra: se um numero mudar, a tabela muda e o teste quebra junto.
 */
public final class Medidas {

    /** A semente das cargas, fixa para o numero sair igual nos tres sistemas. */
    public static final long SEMENTE = 1;

    /** Quantas cargas sorteadas o juiz enfrenta por protocolo. */
    public static final int SORTEADAS = 50;

    /** Quantos acessos tem cada carga sorteada. */
    public static final int ACESSOS_SORTEADOS = 2000;

    private Medidas() {}

    public static List<Cargas.Carga> cargas() {
        return Cargas.todas(SEMENTE);
    }

    /* ===================== 1. correcao ===================== */

    public record Correcao(String protocolo, String carga, int acessos, int leituras,
            int valoresErrados, int estadosInvalidos, String problema) {}

    private static List<Correcao> correcao;

    /**
     * O JUIZ CONTRA OS TRES PROTOCOLOS, EM TODAS AS CARGAS.
     *
     * <p>Duas conferencias por acesso: o valor lido tem de ser o que uma memoria sem cache
     * nenhum devolveria, e a invariante da coerencia tem de valer no estado que ficou.
     */
    public static List<Correcao> correcao() {
        if (correcao != null) return correcao;

        List<Correcao> saida = new ArrayList<>();

        for (Protocolo protocolo : Protocolo.todos())
            for (Cargas.Carga carga : cargas()) {
                Juiz.Veredito veredito = Juiz.julgar(protocolo, carga);

                saida.add(new Correcao(protocolo.nome(), carga.nome(), veredito.acessos(),
                        veredito.leituras(), veredito.valoresErrados(),
                        veredito.estadosInvalidos(), veredito.primeiroProblema()));
            }

        return correcao = List.copyOf(saida);
    }

    public record Sorteadas(String protocolo, int cargas, int acessos, int erradas) {}

    private static List<Sorteadas> sorteadas;

    /** As cargas escritas a mao foram escolhidas por mim; estas, nao. */
    public static List<Sorteadas> sorteadas() {
        if (sorteadas != null) return sorteadas;

        List<Sorteadas> saida = new ArrayList<>();

        for (Protocolo protocolo : Protocolo.todos()) {
            int erradas = 0;

            for (int semente = 1; semente <= SORTEADAS; semente++) {
                Cargas.Carga carga =
                        Cargas.sorteada(new Sorteio(semente), ACESSOS_SORTEADOS);

                if (!Juiz.julgar(protocolo, carga).certo()) erradas++;
            }

            saida.add(new Sorteadas(protocolo.nome(), SORTEADAS,
                    SORTEADAS * ACESSOS_SORTEADOS, erradas));
        }

        return sorteadas = List.copyOf(saida);
    }

    /* ===================== 2. transacoes ===================== */

    public record Transacoes(String protocolo, String carga, int acessos, long leituras,
            long leiturasExclusivas, long promocoes, long escritasDeVolta, double taxaDeAcerto) {

        public long total() {
            return leituras + leiturasExclusivas + promocoes + escritasDeVolta;
        }

        public double porAcesso() {
            return total() / (double) acessos;
        }
    }

    private static List<Transacoes> transacoes;

    public static List<Transacoes> transacoes() {
        if (transacoes != null) return transacoes;

        List<Transacoes> saida = new ArrayList<>();

        for (Protocolo protocolo : Protocolo.todos())
            for (Cargas.Carga carga : cargas()) {
                Maquina maquina = Juiz.rodar(protocolo, carga);

                saida.add(new Transacoes(protocolo.nome(), carga.nome(),
                        carga.acessos().size(), maquina.barramento().leituras(),
                        maquina.barramento().leiturasExclusivas(),
                        maquina.barramento().promocoes(),
                        maquina.barramento().escritasDeVolta(), maquina.taxaDeAcerto()));
            }

        return transacoes = List.copyOf(saida);
    }

    public static Transacoes transacoes(String protocolo, String carga) {
        return transacoes().stream()
                .filter(linha -> linha.protocolo().equals(protocolo))
                .filter(linha -> linha.carga().equals(carga))
                .findFirst().orElseThrow();
    }

    /* ===================== 3. o estado exclusivo ===================== */

    public record Exclusivo(int linhasPorNucleo, int linhas, int acessos, long promocoesMsi,
            long promocoesMesi, long totalMsi, long totalMesi) {

        public long promocoesEvitadas() {
            return promocoesMsi - promocoesMesi;
        }

        public double fracaoDosAcessos() {
            return promocoesEvitadas() / (double) acessos;
        }

        public double fracaoDasTransacoes() {
            return promocoesEvitadas() / (double) totalMsi;
        }
    }

    /** Quantas linhas por nucleo entram na tabela do exclusivo. */
    public static final int[] LINHAS_POR_NUCLEO = {1, 2, 4, 8, 16};

    private static List<Exclusivo> exclusivo;

    /**
     * O QUE O ESTADO EXCLUSIVO ECONOMIZA, E DE QUE ISSO DEPENDE.
     *
     * <p>A tabela varia o numero de linhas privadas de cada nucleo com o numero de acessos
     * fixo. Se a economia fosse por acesso, as colunas andariam juntas; se for por linha,
     * a economia sobe com as linhas e a fracao dos acessos sobe junto.
     */
    public static List<Exclusivo> exclusivo() {
        if (exclusivo != null) return exclusivo;

        List<Exclusivo> saida = new ArrayList<>();

        for (int linhasPorNucleo : LINHAS_POR_NUCLEO) {
            Cargas.Carga carga = Cargas.privadaCom(linhasPorNucleo, new Sorteio(SEMENTE));

            Maquina comMsi = Juiz.rodar(new Msi(), carga);
            Maquina comMesi = Juiz.rodar(new Mesi(), carga);

            saida.add(new Exclusivo(linhasPorNucleo, linhasPorNucleo * Cargas.NUCLEOS,
                    carga.acessos().size(), comMsi.barramento().promocoes(),
                    comMesi.barramento().promocoes(), comMsi.barramento().total(),
                    comMesi.barramento().total()));
        }

        return exclusivo = List.copyOf(saida);
    }

    /* ===================== 4. o estado dono ===================== */

    public record Dono(int consumidores, int acessos, long escritasDeVoltaMesi,
            long escritasDeVoltaMoesi, long totalMesi, long totalMoesi,
            long transferenciasMoesi) {

        public double economia() {
            return 1 - totalMoesi / (double) totalMesi;
        }
    }

    /** Quantos consumidores entram na tabela do dono. */
    public static final int[] CONSUMIDORES = {1, 2, 3};

    private static List<Dono> dono;

    public static List<Dono> dono() {
        if (dono != null) return dono;

        List<Dono> saida = new ArrayList<>();

        for (int consumidores : CONSUMIDORES) {
            Cargas.Carga carga = Cargas.produtorCom(consumidores, new Sorteio(SEMENTE));

            Maquina comMesi = Juiz.rodar(new Mesi(), carga);
            Maquina comMoesi = Juiz.rodar(new Moesi(), carga);

            saida.add(new Dono(consumidores, carga.acessos().size(),
                    comMesi.barramento().escritasDeVolta(),
                    comMoesi.barramento().escritasDeVolta(),
                    comMesi.barramento().total(), comMoesi.barramento().total(),
                    comMoesi.barramento().transferenciasEntreCaches()));
        }

        return dono = List.copyOf(saida);
    }

    /* ===================== 5. compartilhamento falso ===================== */

    public record Falso(int nucleos, int acessos, long transacoesJuntos,
            long transacoesSeparados, double acertoJuntos, double acertoSeparados) {

        public double quantasVezesPior() {
            return transacoesJuntos / (double) transacoesSeparados;
        }
    }

    /** Quantos nucleos brigam pela mesma linha, na tabela do compartilhamento falso. */
    public static final int[] NUCLEOS_NA_LINHA = {2, 3, 4};

    private static List<Falso> falso;

    /**
     * O COMPARTILHAMENTO FALSO E O ENCHIMENTO QUE O CONSERTA.
     *
     * <p>As duas cargas comparadas tem exatamente as mesmas escritas, na mesma ordem, na
     * mesma quantidade de dados. A unica diferenca e onde os dados caem: juntos na mesma
     * linha, ou um em cada linha.
     *
     * <p>Nenhum nucleo le ou escreve o dado do outro em nenhuma das duas. Para o programa,
     * elas sao identicas.
     */
    public static List<Falso> falso() {
        if (falso != null) return falso;

        List<Falso> saida = new ArrayList<>();

        for (int nucleos : NUCLEOS_NA_LINHA) {
            Cargas.Carga juntos = Cargas.falsaCom(nucleos, new Sorteio(SEMENTE));
            Cargas.Carga separados = Cargas.comEnchimentoCom(nucleos, new Sorteio(SEMENTE));

            Maquina comJuntos = Juiz.rodar(new Mesi(), juntos);
            Maquina comSeparados = Juiz.rodar(new Mesi(), separados);

            saida.add(new Falso(nucleos, juntos.acessos().size(),
                    comJuntos.barramento().total(), comSeparados.barramento().total(),
                    comJuntos.taxaDeAcerto(), comSeparados.taxaDeAcerto()));
        }

        return falso = List.copyOf(saida);
    }

    public static Falso falso(int nucleos) {
        return falso().stream().filter(linha -> linha.nucleos() == nucleos)
                .findFirst().orElseThrow();
    }
}
