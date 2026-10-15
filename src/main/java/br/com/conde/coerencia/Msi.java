package br.com.conde.coerencia;

/**
 * MSI: tres estados, modificado, compartilhado e invalido.
 *
 * <p>E o protocolo minimo que mantem a coerencia, e ele e minimo de verdade: tirar qualquer
 * um dos tres estados quebra alguma coisa.
 *
 * <p>O que falta nele e saber a diferenca entre "compartilhado com alguem" e "compartilhado
 * com ninguem". Um cache que leu um bloco que mais ninguem tem fica em COMPARTILHADO do
 * mesmo jeito, e quando for escrever precisa pedir ao barramento uma permissao que ninguem
 * ia disputar.
 */
public final class Msi implements Protocolo {

    @Override
    public String nome() {
        return "MSI";
    }

    @Override
    public boolean temExclusivo() {
        return false;
    }

    @Override
    public boolean temDono() {
        return false;
    }
}
