package br.com.conde.coerencia;

import java.util.List;

/**
 * OS TRES PROTOCOLOS, E A DIFERENCA INTEIRA ENTRE ELES.
 *
 * <p>Os tres sao de invalidacao: quem vai escrever manda os outros jogarem fora a copia, em
 * vez de mandar o valor novo para todo mundo. A alternativa, atualizar em vez de invalidar,
 * e outra familia e nao esta aqui.
 *
 * <p>O que separa MSI, MESI e MOESI sao DOIS BITS, e eles respondem duas perguntas:
 *
 * <ul>
 *   <li>Existe o estado EXCLUSIVO? Com ele, o cache que leu um bloco que ninguem mais tem
 *       sabe disso, e a escrita seguinte nao precisa avisar ninguem. Sem ele, toda escrita
 *       sobre uma linha compartilhada gasta uma transacao, mesmo que nao haja com quem
 *       compartilhar.
 *   <li>Existe o estado DONO? Com ele, o cache que tem a linha suja pode entrega-la a outro
 *       e continuar responsavel por ela, sem escrever de volta na memoria.
 * </ul>
 *
 * <p>Dizer isso e facil. A tabela mede quanto cada um desses dois bits vale, e a resposta
 * depende inteiramente da carga.
 */
public interface Protocolo {

    String nome();

    /** O cache que le um bloco que ninguem mais tem fica em EXCLUSIVO ou em COMPARTILHADO? */
    boolean temExclusivo();

    /** Quem entrega uma linha suja a outro pode continuar dono, sem escrever de volta? */
    boolean temDono();

    static List<Protocolo> todos() {
        return List.of(new Msi(), new Mesi(), new Moesi());
    }
}
