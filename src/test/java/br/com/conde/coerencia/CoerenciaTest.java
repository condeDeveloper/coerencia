package br.com.conde.coerencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** As pecas, uma a uma, antes de qualquer medida. */
class CoerenciaTest {

    // ───────────────────── o sorteio ─────────────────────

    @Nested
    @DisplayName("o sorteio da casa")
    class OSorteio {

        private long estado;

        private int proximoSemDeslocamento() {
            estado = estado * 6364136223846793005L + 1442695040888963407L;

            return (int) estado;
        }

        private int ateSemDeslocamento(int quantos) {
            return Math.floorMod(proximoSemDeslocamento(), quantos);
        }

        @Test
        @DisplayName("o sorteio da os mesmos numeros em qualquer maquina")
        void oSorteioEhFixo() {
            Sorteio sorteio = new Sorteio(1);

            assertThat(List.of(sorteio.proximo(), sorteio.proximo(), sorteio.proximo()))
                    .containsExactly(1817669548, -2107078989, -1510284903);
        }

        /**
         * O NUMERO DE NUCLEOS E UMA POTENCIA DE DOIS, E E ELE QUE O SORTEIO ESCOLHE.
         *
         * <p>Com os bits de baixo, {@code ate(4)} devolve os dois ultimos bits do estado,
         * que percorrem 0, 1, 2, 3 e voltam. Quem acessa vira um rodizio perfeito.
         */
        @Test
        @DisplayName("sem o deslocamento, o nucleo que acessa vira um rodizio")
        void semODeslocamentoONucleoViraRodizio() {
            estado = Medidas.SEMENTE;

            List<Integer> saida = new ArrayList<>();

            for (int i = 0; i < 12; i++) saida.add(ateSemDeslocamento(Cargas.NUCLEOS));

            assertThat(saida.subList(0, 4)).containsExactlyInAnyOrder(0, 1, 2, 3);
            assertThat(saida.subList(4, 8)).isEqualTo(saida.subList(0, 4));
            assertThat(saida.subList(8, 12)).isEqualTo(saida.subList(0, 4));
        }

        /**
         * E O QUE ISSO FARIA COM A MEDIDA.
         *
         * <p>Num rodizio perfeito, nenhum nucleo acessa duas vezes seguidas, e dois acessos
         * seguidos do mesmo nucleo e exatamente o padrao que o estado exclusivo aproveita.
         * A carga sorteada ficaria sendo o pior caso disfarcado de caso qualquer.
         */
        @Test
        @DisplayName("e com isso nenhum nucleo acessaria duas vezes seguidas")
        void oRodizioAcabaComOPadraoQueImporta() {
            estado = Medidas.SEMENTE;

            int seguidosSemDeslocamento = 0;

            int anterior = -1;

            for (int i = 0; i < 2000; i++) {
                int nucleo = ateSemDeslocamento(Cargas.NUCLEOS);

                if (nucleo == anterior) seguidosSemDeslocamento++;

                anterior = nucleo;
            }

            assertThat(seguidosSemDeslocamento).isZero();

            Sorteio sorteio = new Sorteio(Medidas.SEMENTE);

            int seguidos = 0;

            anterior = -1;

            for (int i = 0; i < 2000; i++) {
                int nucleo = sorteio.ate(Cargas.NUCLEOS);

                if (nucleo == anterior) seguidos++;

                anterior = nucleo;
            }

            assertThat(seguidos)
                    .as("com os bits de cima, um quarto dos acessos repete o nucleo")
                    .isBetween(400, 600);
        }
    }

    // ───────────────────── os estados ─────────────────────

    @Nested
    @DisplayName("os estados da linha")
    class OsEstados {

        @Test
        @DisplayName("so o modificado e o exclusivo deixam escrever sem avisar")
        void quemEscreveSemAvisar() {
            assertThat(Estado.MODIFICADO.deixaEscreverSemAvisar()).isTrue();
            assertThat(Estado.EXCLUSIVO.deixaEscreverSemAvisar()).isTrue();

            assertThat(Estado.COMPARTILHADO.deixaEscreverSemAvisar()).isFalse();
            assertThat(Estado.DONO.deixaEscreverSemAvisar()).isFalse();
            assertThat(Estado.INVALIDO.deixaEscreverSemAvisar()).isFalse();
        }

        @Test
        @DisplayName("sujo e quem tem a versao boa com a memoria velha")
        void quemEstaSujo() {
            assertThat(Estado.MODIFICADO.sujo()).isTrue();
            assertThat(Estado.DONO.sujo()).isTrue();

            assertThat(Estado.EXCLUSIVO.sujo())
                    .as("o exclusivo e limpo: igual a memoria, e so dele")
                    .isFalse();
        }

        @Test
        @DisplayName("os tres protocolos sao dois bits de diferenca")
        void osTresSaoDoisBits() {
            assertThat(new Msi().temExclusivo()).isFalse();
            assertThat(new Msi().temDono()).isFalse();

            assertThat(new Mesi().temExclusivo()).isTrue();
            assertThat(new Mesi().temDono()).isFalse();

            assertThat(new Moesi().temExclusivo()).isTrue();
            assertThat(new Moesi().temDono()).isTrue();
        }
    }

    // ───────────────────── a maquina ─────────────────────

    @Nested
    @DisplayName("a maquina")
    class AMaquina {

        private Maquina nova(Protocolo protocolo) {
            return new Maquina(protocolo, 4, 8, 4);
        }

        @Test
        @DisplayName("a primeira leitura custa uma transacao e as seguintes nao custam nada")
        void aPrimeiraLeituraCusta() {
            Maquina maquina = nova(new Mesi());

            maquina.ler(0, 0, 0);

            assertThat(maquina.barramento().total()).isEqualTo(1);

            maquina.ler(0, 0, 1);
            maquina.ler(0, 0, 2);

            assertThat(maquina.barramento().total()).isEqualTo(1);
            assertThat(maquina.taxaDeAcerto()).isEqualTo(2 / 3.0);
        }

        /**
         * A DIFERENCA ENTRE MSI E MESI, NO MENOR EXEMPLO QUE EXISTE.
         *
         * <p>Ler e depois escrever uma linha que ninguem mais tem. No MESI a leitura deixa
         * a linha em EXCLUSIVO e a escrita nao atravessa o barramento. No MSI ela fica em
         * COMPARTILHADO e a escrita precisa pedir uma promocao que ninguem ia disputar.
         */
        @Test
        @DisplayName("ler e escrever um bloco privado: duas transacoes no MSI, uma no MESI")
        void lerEEscreverUmBlocoPrivado() {
            Maquina comMsi = nova(new Msi());

            comMsi.ler(0, 0, 0);
            comMsi.escrever(0, 0, 0, 7);

            assertThat(comMsi.barramento().total()).isEqualTo(2);
            assertThat(comMsi.barramento().promocoes()).isEqualTo(1);

            Maquina comMesi = nova(new Mesi());

            comMesi.ler(0, 0, 0);
            comMesi.escrever(0, 0, 0, 7);

            assertThat(comMesi.barramento().total()).isEqualTo(1);
            assertThat(comMesi.barramento().promocoes()).isZero();

            assertThat(comMesi.estado(0, 0)).isEqualTo(Estado.MODIFICADO);
        }

        @Test
        @DisplayName("escrever invalida a copia de todo mundo")
        void escreverInvalidaOsOutros() {
            Maquina maquina = nova(new Mesi());

            maquina.ler(0, 0, 0);
            maquina.ler(1, 0, 0);
            maquina.ler(2, 0, 0);

            assertThat(maquina.estado(0, 0)).isEqualTo(Estado.COMPARTILHADO);

            maquina.escrever(3, 0, 0, 9);

            assertThat(maquina.estado(0, 0)).isEqualTo(Estado.INVALIDO);
            assertThat(maquina.estado(1, 0)).isEqualTo(Estado.INVALIDO);
            assertThat(maquina.estado(2, 0)).isEqualTo(Estado.INVALIDO);
            assertThat(maquina.estado(3, 0)).isEqualTo(Estado.MODIFICADO);
        }

        /**
         * A DIFERENCA ENTRE MESI E MOESI.
         *
         * <p>Um nucleo escreve, outro le. No MESI a linha suja precisa ir para a memoria
         * antes de ser compartilhada; no MOESI ela vai direto de um cache para o outro, e
         * quem escreveu continua DONO.
         */
        @Test
        @DisplayName("entregar uma linha suja: escrita de volta no MESI, nenhuma no MOESI")
        void entregarUmaLinhaSuja() {
            Maquina comMesi = nova(new Mesi());

            comMesi.escrever(0, 0, 0, 5);
            comMesi.ler(1, 0, 0);

            assertThat(comMesi.barramento().escritasDeVolta()).isEqualTo(1);
            assertThat(comMesi.estado(0, 0)).isEqualTo(Estado.COMPARTILHADO);

            Maquina comMoesi = nova(new Moesi());

            comMoesi.escrever(0, 0, 0, 5);
            comMoesi.ler(1, 0, 0);

            assertThat(comMoesi.barramento().escritasDeVolta()).isZero();
            assertThat(comMoesi.estado(0, 0)).isEqualTo(Estado.DONO);
            assertThat(comMoesi.barramento().transferenciasEntreCaches()).isEqualTo(1);
        }

        @Test
        @DisplayName("o dono escreve de volta quando alguem o invalida")
        void oDonoEscreveDeVoltaDepois() {
            Maquina maquina = nova(new Moesi());

            maquina.escrever(0, 0, 0, 5);
            maquina.ler(1, 0, 0);

            assertThat(maquina.barramento().escritasDeVolta()).isZero();

            maquina.escrever(2, 0, 0, 6);

            assertThat(maquina.barramento().escritasDeVolta()).isEqualTo(1);
        }

        @Test
        @DisplayName("a invariante vale depois de cada passo, nos tres protocolos")
        void aInvarianteVale() {
            for (Protocolo protocolo : Protocolo.todos()) {
                Maquina maquina = nova(protocolo);

                Sorteio sorteio = new Sorteio(5);

                for (int passo = 0; passo < 2000; passo++) {
                    int nucleo = sorteio.ate(4);
                    int linha = sorteio.ate(8);
                    int palavra = sorteio.ate(4);

                    if (sorteio.com(0.5)) maquina.escrever(nucleo, linha, palavra, passo);
                    else maquina.ler(nucleo, linha, palavra);

                    assertThat(maquina.violacoes())
                            .as(protocolo.nome() + " no passo " + passo)
                            .isEmpty();
                }
            }
        }
    }

    // ───────────────────── as cargas ─────────────────────

    @Nested
    @DisplayName("as cargas")
    class AsCargas {

        @Test
        @DisplayName("o falso e o com enchimento tem as mesmas escritas, na mesma ordem")
        void asDuasCargasSaoAMesmaCoisa() {
            List<Acesso> juntos = Cargas.falsa(new Sorteio(Medidas.SEMENTE)).acessos();
            List<Acesso> separados =
                    Cargas.comEnchimento(new Sorteio(Medidas.SEMENTE)).acessos();

            assertThat(juntos).hasSameSizeAs(separados);

            for (int i = 0; i < juntos.size(); i++) {
                assertThat(juntos.get(i).nucleo()).isEqualTo(separados.get(i).nucleo());
                assertThat(juntos.get(i).valor()).isEqualTo(separados.get(i).valor());
                assertThat(juntos.get(i).escrita()).isTrue();
                assertThat(separados.get(i).escrita()).isTrue();
            }
        }

        @Test
        @DisplayName("na carga privada, nenhum nucleo toca na linha de outro")
        void aPrivadaEhMesmoPrivada() {
            int[] dono = new int[Cargas.LINHAS];

            java.util.Arrays.fill(dono, -1);

            for (Acesso acesso : Cargas.privada(new Sorteio(Medidas.SEMENTE)).acessos()) {
                if (dono[acesso.linha()] < 0) dono[acesso.linha()] = acesso.nucleo();

                assertThat(acesso.nucleo())
                        .as("linha " + acesso.linha())
                        .isEqualTo(dono[acesso.linha()]);
            }
        }

        @Test
        @DisplayName("na carga so de leitura ninguem escreve")
        void aSoLeituraNaoEscreve() {
            for (Acesso acesso : Cargas.soLeitura(new Sorteio(Medidas.SEMENTE)).acessos())
                assertThat(acesso.escrita()).isFalse();
        }
    }
}
