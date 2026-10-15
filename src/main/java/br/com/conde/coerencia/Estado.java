package br.com.conde.coerencia;

/**
 * O ESTADO DE UMA LINHA NUM CACHE.
 *
 * <p>Sao cinco, e cada protocolo usa um subconjunto. A diferenca entre MSI, MESI e MOESI
 * cabe inteira em quais destes cinco estados existem, e e isso que este repositorio mede.
 *
 * <ul>
 *   <li>INVALIDO: este cache nao tem a linha, ou tem uma copia que nao vale mais.
 *   <li>COMPARTILHADO: a linha e igual a da memoria, e outros podem ter copia.
 *   <li>EXCLUSIVO: igual a da memoria, e mais ninguem tem copia. Existe so no MESI e no
 *       MOESI, e e um estado LIMPO: escrever nele nao precisa avisar ninguem.
 *   <li>MODIFICADO: este cache mudou a linha e e o unico que a tem. A memoria esta velha.
 *   <li>DONO: como o modificado, mas com copias compartilhadas por ai. Existe so no MOESI, e
 *       serve para adiar a escrita de volta.
 * </ul>
 */
public enum Estado {

    INVALIDO("I"),
    COMPARTILHADO("S"),
    EXCLUSIVO("E"),
    MODIFICADO("M"),
    DONO("O");

    private final String letra;

    Estado(String letra) {
        this.letra = letra;
    }

    public String letra() {
        return letra;
    }

    /** O cache pode responder uma leitura sem ir ao barramento? */
    public boolean valido() {
        return this != INVALIDO;
    }

    /** Este cache tem a versao boa, e a memoria esta velha? */
    public boolean sujo() {
        return this == MODIFICADO || this == DONO;
    }

    /** Escrever aqui precisa avisar alguem? */
    public boolean deixaEscreverSemAvisar() {
        return this == MODIFICADO || this == EXCLUSIVO;
    }
}
