package br.com.conde.coerencia;

import java.util.ArrayList;
import java.util.List;

/**
 * A MAQUINA: varios nucleos, um cache em cada, uma memoria no fim.
 *
 * <p>Cada cache guarda LINHAS inteiras, e a linha tem varias palavras. Essa e a decisao de
 * hardware que esta por tras de metade do repositorio: a coerencia e mantida por linha, nao
 * por palavra, e por isso dois nucleos que escrevem em palavras diferentes da mesma linha
 * brigam como se escrevessem na mesma.
 *
 * <p>O cache aqui e de capacidade infinita, de proposito. Uma linha so sai do cache quando
 * outro nucleo manda ela sair, nunca por falta de espaco. Isso separa o que e custo de
 * coerencia do que e custo de capacidade, que e o assunto de outro repositorio.
 */
public final class Maquina {

    private final Protocolo protocolo;
    private final int nucleos;
    private final int linhas;
    private final int palavrasPorLinha;

    private final Estado[][] estado;
    private final int[][][] dados;

    private final int[][] memoria;

    private final Barramento barramento = new Barramento();

    private long acertos;
    private long acessos;

    public Maquina(Protocolo protocolo, int nucleos, int linhas, int palavrasPorLinha) {
        this.protocolo = protocolo;
        this.nucleos = nucleos;
        this.linhas = linhas;
        this.palavrasPorLinha = palavrasPorLinha;

        this.estado = new Estado[nucleos][linhas];
        this.dados = new int[nucleos][linhas][palavrasPorLinha];
        this.memoria = new int[linhas][palavrasPorLinha];

        for (Estado[] doNucleo : estado) java.util.Arrays.fill(doNucleo, Estado.INVALIDO);
    }

    public Protocolo protocolo() {
        return protocolo;
    }

    public Barramento barramento() {
        return barramento;
    }

    public Estado estado(int nucleo, int linha) {
        return estado[nucleo][linha];
    }

    public long acertos() {
        return acertos;
    }

    public long acessos() {
        return acessos;
    }

    public double taxaDeAcerto() {
        return acessos == 0 ? 1 : acertos / (double) acessos;
    }

    /**
     * Le uma palavra.
     *
     * <p>Se a linha ja esta valida neste cache, nada atravessa o barramento. Se nao esta,
     * sai uma leitura: quem tiver a versao suja entrega, e o estado final depende de mais
     * alguem ter copia e de o protocolo ter o estado exclusivo.
     */
    public int ler(int nucleo, int linha, int palavra) {
        acessos++;

        if (estado[nucleo][linha].valido()) {
            acertos++;

            return dados[nucleo][linha][palavra];
        }

        barramento.leitura();

        int[] versaoBoa = memoria[linha];

        boolean alguemTinha = false;
        boolean alguemSujo = false;

        for (int outro = 0; outro < nucleos; outro++) {
            if (outro == nucleo || !estado[outro][linha].valido()) continue;

            alguemTinha = true;

            if (!estado[outro][linha].sujo()) {
                estado[outro][linha] = Estado.COMPARTILHADO;
                continue;
            }

            alguemSujo = true;
            versaoBoa = dados[outro][linha];

            barramento.transferenciaEntreCaches();

            if (protocolo.temDono()) {
                /* O dono entrega a linha e continua responsavel por ela: a memoria segue
                 * velha, e a escrita de volta fica para quando alguem invalidar. */
                estado[outro][linha] = Estado.DONO;
            } else {
                memoria[linha] = versaoBoa.clone();

                barramento.escritaDeVolta();

                estado[outro][linha] = Estado.COMPARTILHADO;
            }
        }

        dados[nucleo][linha] = versaoBoa.clone();

        estado[nucleo][linha] = !alguemTinha && protocolo.temExclusivo()
                ? Estado.EXCLUSIVO : Estado.COMPARTILHADO;

        /* Sem o estado de dono, quem acabou de receber de um cache sujo fica compartilhado
         * com a memoria ja atualizada; com ele, a memoria continua velha. */
        if (alguemSujo && protocolo.temDono()) estado[nucleo][linha] = Estado.COMPARTILHADO;

        return dados[nucleo][linha][palavra];
    }

    /**
     * Escreve uma palavra.
     *
     * <p>Tres caminhos, e e neles que os protocolos se separam: quem ja esta em MODIFICADO
     * ou EXCLUSIVO escreve sem avisar ninguem; quem esta em COMPARTILHADO ou DONO pede uma
     * PROMOCAO, que nao carrega dado nenhum; quem nao tem a linha pede uma leitura
     * exclusiva.
     */
    public void escrever(int nucleo, int linha, int palavra, int valor) {
        acessos++;

        Estado meu = estado[nucleo][linha];

        if (meu.deixaEscreverSemAvisar()) {
            acertos++;

            dados[nucleo][linha][palavra] = valor;
            estado[nucleo][linha] = Estado.MODIFICADO;

            return;
        }

        if (meu.valido()) {
            acertos++;

            barramento.promocao();

            invalidarOsOutros(nucleo, linha);

            dados[nucleo][linha][palavra] = valor;
            estado[nucleo][linha] = Estado.MODIFICADO;

            return;
        }

        barramento.leituraExclusiva();

        int[] versaoBoa = memoria[linha];

        for (int outro = 0; outro < nucleos; outro++) {
            if (outro == nucleo || !estado[outro][linha].valido()) continue;

            if (estado[outro][linha].sujo()) {
                versaoBoa = dados[outro][linha];

                barramento.transferenciaEntreCaches();
            }
        }

        dados[nucleo][linha] = versaoBoa.clone();

        invalidarOsOutros(nucleo, linha);

        dados[nucleo][linha][palavra] = valor;
        estado[nucleo][linha] = Estado.MODIFICADO;
    }

    /**
     * Manda todo mundo jogar fora a copia.
     *
     * <p>Quem estava sujo precisa escrever de volta antes de sumir, se nao houver quem
     * assuma a linha. Esta e a escrita de volta que o estado de dono adia.
     */
    private void invalidarOsOutros(int nucleo, int linha) {
        for (int outro = 0; outro < nucleos; outro++) {
            if (outro == nucleo || !estado[outro][linha].valido()) continue;

            if (estado[outro][linha].sujo()) {
                memoria[linha] = dados[outro][linha].clone();

                barramento.escritaDeVolta();
            }

            estado[outro][linha] = Estado.INVALIDO;
        }
    }

    /** Escreve de volta tudo o que ainda esta sujo, que e o fim de qualquer execucao. */
    public void esvaziar() {
        for (int linha = 0; linha < linhas; linha++)
            for (int nucleo = 0; nucleo < nucleos; nucleo++)
                if (estado[nucleo][linha].sujo()) {
                    memoria[linha] = dados[nucleo][linha].clone();

                    barramento.escritaDeVolta();

                    estado[nucleo][linha] = Estado.COMPARTILHADO;
                }
    }

    public int naMemoria(int linha, int palavra) {
        return memoria[linha][palavra];
    }

    /**
     * A INVARIANTE DA COERENCIA, conferida depois de cada acesso.
     *
     * <p>Ela e o contrato inteiro do protocolo, e cabe em tres frases. No maximo um cache
     * pode estar com a linha em estado de escrita. Se alguem esta, mais ninguem tem copia
     * valida, a nao ser que ele seja o DONO, e ai as copias sao iguais a dele. E toda copia
     * limpa e igual a da memoria.
     */
    public List<String> violacoes() {
        List<String> saida = new ArrayList<>();

        for (int linha = 0; linha < linhas; linha++) {
            int escritores = 0;
            int donos = 0;
            int validos = 0;

            int quemEscreve = -1;

            for (int nucleo = 0; nucleo < nucleos; nucleo++) {
                Estado atual = estado[nucleo][linha];

                if (atual.valido()) validos++;

                if (atual == Estado.MODIFICADO || atual == Estado.EXCLUSIVO) {
                    escritores++;
                    quemEscreve = nucleo;
                }

                if (atual == Estado.DONO) {
                    donos++;
                    quemEscreve = nucleo;
                }
            }

            if (escritores + donos > 1)
                saida.add("linha " + linha + ": " + (escritores + donos) + " escritores");

            if (escritores == 1 && validos > 1)
                saida.add("linha " + linha + ": copia valida junto com um modificado");

            int[] referencia = quemEscreve >= 0 ? dados[quemEscreve][linha] : memoria[linha];

            for (int nucleo = 0; nucleo < nucleos; nucleo++) {
                if (!estado[nucleo][linha].valido()) continue;

                if (java.util.Arrays.equals(dados[nucleo][linha], referencia)) continue;

                saida.add("linha " + linha + ": copias diferentes no nucleo " + nucleo);
            }
        }

        return saida;
    }
}
