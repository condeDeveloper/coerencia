package br.com.conde.coerencia;

/**
 * MOESI: o MESI com o estado DONO.
 *
 * <p>Quando um cache com a linha MODIFICADA precisa entregar a outro que esta lendo, o MESI
 * escreve a linha de volta na memoria e os dois ficam em COMPARTILHADO. O MOESI entrega a
 * linha direto ao outro e fica em DONO: a memoria continua velha, e ele continua responsavel
 * por entregar a quem pedir e por escrever de volta no fim.
 *
 * <p>O padrao que isso ataca e o produtor e consumidor: um nucleo escreve, outro le, e a
 * memoria nao precisa participar.
 */
public final class Moesi implements Protocolo {

    @Override
    public String nome() {
        return "MOESI";
    }

    @Override
    public boolean temExclusivo() {
        return true;
    }

    @Override
    public boolean temDono() {
        return true;
    }
}
