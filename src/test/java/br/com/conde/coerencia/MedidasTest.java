package br.com.conde.coerencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * TODO NUMERO DO README ESTA AQUI.
 *
 * <p>Texto nao quebra quando deixa de ser verdade. Teste quebra. Se alguma medida mudar,
 * este arquivo e a tabela do README caem juntos, e nenhum numero fica no texto sozinho.
 */
class MedidasTest {

    private static final List<Medidas.Correcao> CORRECAO = Medidas.correcao();
    private static final List<Medidas.Sorteadas> SORTEADAS = Medidas.sorteadas();
    private static final List<Medidas.Transacoes> TRANSACOES = Medidas.transacoes();
    private static final List<Medidas.Exclusivo> EXCLUSIVO = Medidas.exclusivo();
    private static final List<Medidas.Dono> DONO = Medidas.dono();

    private static Medidas.Exclusivo exclusivo(int linhasPorNucleo) {
        return EXCLUSIVO.stream()
                .filter(linha -> linha.linhasPorNucleo() == linhasPorNucleo)
                .findFirst().orElseThrow();
    }

    private static Medidas.Dono dono(int consumidores) {
        return DONO.stream().filter(linha -> linha.consumidores() == consumidores)
                .findFirst().orElseThrow();
    }

    // ───────────────────── 1. correcao ─────────────────────

    @Nested
    @DisplayName("tabela 1: toda leitura devolve o que a memoria serial diz")
    class ACorrecao {

        @Test
        @DisplayName("sao 3 protocolos vezes 6 cargas, e nenhum valor errado")
        void nenhumValorErrado() {
            assertThat(CORRECAO).hasSize(18);

            for (Medidas.Correcao linha : CORRECAO) {
                assertThat(linha.valoresErrados())
                        .as(linha.protocolo() + " em " + linha.carga())
                        .isZero();

                assertThat(linha.problema()).isEmpty();
            }
        }

        @Test
        @DisplayName("e nenhum estado invalido, conferido depois de cada acesso")
        void nenhumEstadoInvalido() {
            for (Medidas.Correcao linha : CORRECAO)
                assertThat(linha.estadosInvalidos())
                        .as(linha.protocolo() + " em " + linha.carga())
                        .isZero();
        }

        @Test
        @DisplayName("cada carga tem 4.000 acessos, e as de escrita nao tem leitura nenhuma")
        void oTamanhoDasCargas() {
            for (Medidas.Correcao linha : CORRECAO) {
                assertThat(linha.acessos()).isBetween(3999, 4000);

                if (!linha.carga().contains("falso") && !linha.carga().contains("enchimento"))
                    continue;

                assertThat(linha.leituras())
                        .as(linha.carga() + " e so escrita")
                        .isZero();
            }
        }

        @Test
        @DisplayName("e 50 cargas sorteadas por protocolo, 100.000 acessos cada lote")
        void asCargasSorteadas() {
            assertThat(SORTEADAS).hasSize(3);

            for (Medidas.Sorteadas linha : SORTEADAS) {
                assertThat(linha.cargas()).isEqualTo(50);
                assertThat(linha.acessos()).isEqualTo(100_000);
                assertThat(linha.erradas()).as(linha.protocolo()).isZero();
            }
        }
    }

    // ───────────────────── 2. transacoes ─────────────────────

    @Nested
    @DisplayName("tabela 2: quantas transacoes cada protocolo gasta")
    class AsTransacoes {

        @Test
        @DisplayName("na carga privada, o MSI gasta 50% mais que o MESI")
        void naPrivadaOMsiGastaMais() {
            assertThat(Medidas.transacoes("MSI", "privada").porAcesso())
                    .isCloseTo(0.0480, within(0.0001));

            assertThat(Medidas.transacoes("MESI", "privada").porAcesso())
                    .isCloseTo(0.0320, within(0.0001));
        }

        @Test
        @DisplayName("na carga so de leitura, os tres sao identicos")
        void naSoLeituraOsTresEmpatam() {
            for (Protocolo protocolo : Protocolo.todos())
                assertThat(Medidas.transacoes(protocolo.nome(), "so leitura").porAcesso())
                        .as(protocolo.nome())
                        .isCloseTo(0.0640, within(0.0001));
        }

        /**
         * A CARGA MIGRATORIA E O PIOR CASO, E NENHUM DOS DOIS BITS AJUDA.
         *
         * <p>1,1445 transacoes por acesso no MSI e 1,1285 no MESI: o estado exclusivo
         * economiza 64 promocoes em 1.526, que sao as primeiras leituras de cada linha. O
         * estado dono nao muda nada, porque quem recebe a linha vai escrever nela e
         * invalidar o dono de qualquer jeito.
         */
        @Test
        @DisplayName("na migratoria os dois bits quase nao servem")
        void naMigratoriaOsBitsNaoServem() {
            assertThat(Medidas.transacoes("MSI", "migratoria").porAcesso())
                    .isCloseTo(1.1445, within(0.0001));

            assertThat(Medidas.transacoes("MESI", "migratoria").porAcesso())
                    .isCloseTo(1.1285, within(0.0001));

            assertThat(Medidas.transacoes("MOESI", "migratoria").porAcesso())
                    .isEqualTo(Medidas.transacoes("MESI", "migratoria").porAcesso());
        }

        @Test
        @DisplayName("no produtor e consumidor, so o MOESI muda alguma coisa")
        void noProdutorSoOMoesiMuda() {
            assertThat(Medidas.transacoes("MSI", "produtor e consumidor").porAcesso())
                    .isCloseTo(1.2500, within(0.0001));

            assertThat(Medidas.transacoes("MESI", "produtor e consumidor").porAcesso())
                    .isCloseTo(1.2500, within(0.0001));

            assertThat(Medidas.transacoes("MOESI", "produtor e consumidor").porAcesso())
                    .isCloseTo(1.0160, within(0.0001));
        }

        @Test
        @DisplayName("no compartilhamento falso os tres gastam exatamente o mesmo")
        void noFalsoNenhumAjuda() {
            for (Protocolo protocolo : Protocolo.todos())
                assertThat(Medidas.transacoes(protocolo.nome(), "compartilhamento falso")
                        .porAcesso())
                        .as(protocolo.nome())
                        .isCloseTo(1.4880, within(0.0001));
        }

        @Test
        @DisplayName("a taxa de acerto nao depende do protocolo, so da carga")
        void aTaxaDeAcertoNaoDependeDoProtocolo() {
            for (Cargas.Carga carga : Medidas.cargas()) {
                double daMsi = Medidas.transacoes("MSI", carga.nome()).taxaDeAcerto();

                for (Protocolo protocolo : Protocolo.todos())
                    assertThat(Medidas.transacoes(protocolo.nome(), carga.nome())
                            .taxaDeAcerto())
                            .as(protocolo.nome() + " em " + carga.nome())
                            .isEqualTo(daMsi);
            }
        }
    }

    // ───────────────────── 3. o estado exclusivo ─────────────────────

    @Nested
    @DisplayName("tabela 3: o que o estado exclusivo economiza")
    class OExclusivo {

        /**
         * UMA TRANSACAO POR LINHA, NAO POR ACESSO.
         *
         * <p>Com o numero de acessos fixo em 4.000, as promocoes evitadas sao 4, 8, 16, 32 e
         * 64: exatamente o numero de linhas privadas. Dobrar as linhas dobra a economia;
         * dobrar os acessos nao muda nada.
         */
        @Test
        @DisplayName("as promocoes evitadas sao exatamente o numero de linhas")
        void aEconomiaEhPorLinha() {
            for (Medidas.Exclusivo linha : EXCLUSIVO) {
                assertThat(linha.promocoesEvitadas())
                        .as(linha.linhasPorNucleo() + " linhas por nucleo")
                        .isEqualTo(linha.linhas());

                assertThat(linha.promocoesMesi())
                        .as("o MESI nao faz promocao nenhuma numa carga privada")
                        .isZero();
            }
        }

        @Test
        @DisplayName("e por isso ela vai de 0,10% a 1,60% dos acessos")
        void aFracaoDosAcessosSobeComAsLinhas() {
            assertThat(exclusivo(1).fracaoDosAcessos()).isCloseTo(0.001, within(0.0001));
            assertThat(exclusivo(16).fracaoDosAcessos()).isCloseTo(0.016, within(0.0001));

            assertThat(exclusivo(16).promocoesEvitadas())
                    .isEqualTo(16 * exclusivo(1).promocoesEvitadas());
        }

        @Test
        @DisplayName("em transacoes, porem, ela e um terco do total")
        void emTransacoesEhUmTerco() {
            for (Medidas.Exclusivo linha : EXCLUSIVO)
                assertThat(linha.fracaoDasTransacoes())
                        .as(linha.linhasPorNucleo() + " linhas por nucleo")
                        .isCloseTo(1 / 3.0, within(0.001));
        }
    }

    // ───────────────────── 4. o estado dono ─────────────────────

    @Nested
    @DisplayName("tabela 4: o que o estado dono economiza")
    class ODono {

        @Test
        @DisplayName("com um consumidor so, o MOESI corta 32,3% das transacoes")
        void comUmConsumidor() {
            assertThat(dono(1).escritasDeVoltaMesi()).isEqualTo(2000);
            assertThat(dono(1).escritasDeVoltaMoesi()).isEqualTo(64);

            assertThat(dono(1).economia()).isCloseTo(0.323, within(0.001));
        }

        /**
         * E A ECONOMIA CAI CONFORME ENTRAM CONSUMIDORES.
         *
         * <p>As escritas de volta evitadas sao uma por producao, e as leituras dos
         * consumidores entram no total sem mudar isso. Quanto mais gente le cada valor, mais
         * o total cresce e menos a economia pesa: 32,3%, 23,8% e 18,7%.
         */
        @Test
        @DisplayName("e com tres consumidores ela cai para 18,7%")
        void comTresConsumidores() {
            assertThat(dono(3).economia()).isCloseTo(0.187, within(0.001));

            assertThat(dono(1).economia()).isGreaterThan(dono(2).economia());
            assertThat(dono(2).economia()).isGreaterThan(dono(3).economia());
        }

        @Test
        @DisplayName("as escritas de volta do MOESI sao so as do esvaziamento final")
        void oMoesiSoEscreveNoFim() {
            for (Medidas.Dono linha : DONO)
                assertThat(linha.escritasDeVoltaMoesi())
                        .as(linha.consumidores() + " consumidores")
                        .isEqualTo(Cargas.LINHAS);
        }
    }

    // ───────────────────── 5. compartilhamento falso ─────────────────────

    @Nested
    @DisplayName("tabela 5: o compartilhamento falso")
    class OFalso {

        /**
         * O MESMO PROGRAMA, 1.018 VEZES MAIS CARO.
         *
         * <p>Duas escritas, em duas palavras diferentes, por dois nucleos que nunca tocam no
         * dado um do outro. Juntas na mesma linha: 4.070 transacoes. Uma em cada linha: 4.
         */
        @Test
        @DisplayName("com dois nucleos na mesma linha, 1.018 vezes mais transacoes")
        void comDoisNucleos() {
            Medidas.Falso linha = Medidas.falso(2);

            assertThat(linha.transacoesJuntos()).isEqualTo(4070);
            assertThat(linha.transacoesSeparados()).isEqualTo(4);
            assertThat(linha.quantasVezesPior()).isCloseTo(1017.5, within(0.5));
        }

        @Test
        @DisplayName("e a taxa de acerto cai de 99,95% para 49,13%")
        void aTaxaDeAcertoDespenca() {
            Medidas.Falso linha = Medidas.falso(2);

            assertThat(linha.acertoJuntos()).isCloseTo(0.4913, within(0.0001));
            assertThat(linha.acertoSeparados()).isCloseTo(0.9995, within(0.0001));
        }

        @Test
        @DisplayName("com quatro nucleos sao 5.952 contra 8, e o acerto cai a 25,6%")
        void comQuatroNucleos() {
            Medidas.Falso linha = Medidas.falso(4);

            assertThat(linha.transacoesJuntos()).isEqualTo(5952);
            assertThat(linha.transacoesSeparados()).isEqualTo(8);
            assertThat(linha.acertoJuntos()).isCloseTo(0.2560, within(0.0001));
        }

        /**
         * E A RAZAO CAI COM MAIS NUCLEOS, QUE E O CONTRARIO DO QUE PARECE.
         *
         * <p>Com mais nucleos, o custo de quem esta junto sobe menos que proporcionalmente,
         * e o custo de quem esta separado sobe por nucleo. A razao vai de 1.018 para 744.
         */
        @Test
        @DisplayName("a razao cai conforme entram nucleos, de 1.018 para 744")
        void aRazaoCaiComMaisNucleos() {
            assertThat(Medidas.falso(2).quantasVezesPior())
                    .isGreaterThan(Medidas.falso(3).quantasVezesPior());

            assertThat(Medidas.falso(3).quantasVezesPior())
                    .isGreaterThan(Medidas.falso(4).quantasVezesPior());

            assertThat(Medidas.falso(4).quantasVezesPior()).isCloseTo(744, within(1.0));
        }
    }
}
