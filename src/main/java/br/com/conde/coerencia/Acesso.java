package br.com.conde.coerencia;

/** Um acesso: quem, onde, e se escreve. */
public record Acesso(int nucleo, int linha, int palavra, boolean escrita, int valor) {

    public static Acesso leitura(int nucleo, int linha, int palavra) {
        return new Acesso(nucleo, linha, palavra, false, 0);
    }

    public static Acesso escrita(int nucleo, int linha, int palavra, int valor) {
        return new Acesso(nucleo, linha, palavra, true, valor);
    }

    @Override
    public String toString() {
        return "n" + nucleo + (escrita ? " escreve " : " le ") + linha + ":" + palavra;
    }
}
