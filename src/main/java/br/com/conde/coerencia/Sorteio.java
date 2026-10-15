package br.com.conde.coerencia;

/**
 * O SORTEIO DA CASA, escrito aqui em vez de usar o da biblioteca.
 *
 * <p>E um gerador congruente linear com as constantes de Knuth. A razao de ser proprio e
 * medida: o teste roda com semente fixa em tres sistemas operacionais e precisa dar o mesmo
 * numero nos tres, e a implementacao da biblioteca nao e contrato entre versoes.
 *
 * <h2>O deslocamento de 32</h2>
 *
 * <p>O estado tem 64 bits e o que sai sao os 32 DE CIMA. Nos bits de baixo, os k ultimos
 * repetem com periodo 2 elevado a k, e aqui o numero de nucleos e o de palavras por linha
 * sao potencias de dois: {@code ate(4)} e {@code ate(8)}.
 *
 * <p>Com os bits de baixo, o nucleo que acessa viraria um rodizio perfeito, 0, 1, 2, 3, e a
 * palavra dentro da linha tambem. A carga sorteada deixaria de ter dois acessos seguidos do
 * mesmo nucleo, que e exatamente o padrao que o estado exclusivo existe para aproveitar: a
 * tabela diria que ele nao serve para nada.
 */
public final class Sorteio {

    private long estado;

    public Sorteio(long semente) {
        this.estado = semente;
    }

    /** Um inteiro qualquer, dos 32 bits de cima do estado. */
    public int proximo() {
        estado = estado * 6364136223846793005L + 1442695040888963407L;

        return (int) (estado >>> 32);
    }

    /** Um valor de 0 ate quantos menos um. */
    public int ate(int quantos) {
        return Math.floorMod(proximo(), quantos);
    }

    /** Verdadeiro com esta probabilidade. */
    public boolean com(double chance) {
        return (proximo() & 0x7fffffff) / (double) 0x80000000L < chance;
    }
}
