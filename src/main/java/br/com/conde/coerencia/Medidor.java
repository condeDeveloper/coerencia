package br.com.conde.coerencia;

import java.util.Locale;

/**
 * O MEDIDOR: imprime as tabelas que o README mostra e que os testes conferem.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros. Texto nao quebra
 * quando deixa de ser verdade; teste quebra.
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String comando = args.length > 0 ? args[0] : "tudo";

        if (comando.equals("correcao") || comando.equals("tudo")) correcao();
        if (comando.equals("transacoes") || comando.equals("tudo")) transacoes();
        if (comando.equals("exclusivo") || comando.equals("tudo")) exclusivo();
        if (comando.equals("dono") || comando.equals("tudo")) dono();
        if (comando.equals("falso") || comando.equals("tudo")) falso();
    }

    private static void correcao() {
        System.out.println();
        System.out.println("TODA LEITURA DEVOLVE O QUE A MEMORIA SERIAL DIZ?");
        System.out.println("O juiz e um vetor, sem cache, sem estado e sem barramento. E a"
                + " invariante da coerencia");
        System.out.println("e conferida depois de CADA acesso, nao so no fim.");
        System.out.println();

        System.out.printf("  %-8s %-24s %10s %10s %16s %18s%n",
                "protocolo", "carga", "acessos", "leituras", "valores errados",
                "estados invalidos");

        for (Medidas.Correcao linha : Medidas.correcao())
            System.out.printf("  %-8s %-24s %10d %10d %16d %18d%n",
                    linha.protocolo(), linha.carga(), linha.acessos(), linha.leituras(),
                    linha.valoresErrados(), linha.estadosInvalidos());

        System.out.println();
        System.out.println("E EM CARGAS QUE NINGUEM ESCOLHEU");
        System.out.println();

        System.out.printf("  %-8s %10s %12s %16s%n",
                "protocolo", "cargas", "acessos", "cargas erradas");

        for (Medidas.Sorteadas linha : Medidas.sorteadas())
            System.out.printf("  %-8s %10d %12d %16d%n",
                    linha.protocolo(), linha.cargas(), linha.acessos(), linha.erradas());

        System.out.println();
    }

    private static void transacoes() {
        System.out.println();
        System.out.println("QUANTAS TRANSACOES DE BARRAMENTO CADA PROTOCOLO GASTA");
        System.out.println("A unidade e a transacao, nunca o segundo: e ela que custa caro"
                + " num sistema de varios nucleos.");
        System.out.println();

        System.out.printf("  %-8s %-24s %12s %10s %10s %10s %10s %10s%n",
                "protocolo", "carga", "por acesso", "leitura", "exclusiva", "promocao",
                "de volta", "acerto");

        for (Medidas.Transacoes linha : Medidas.transacoes())
            System.out.printf(Locale.ROOT, "  %-8s %-24s %12.4f %10d %10d %10d %10d %10.4f%n",
                    linha.protocolo(), linha.carga(), linha.porAcesso(), linha.leituras(),
                    linha.leiturasExclusivas(), linha.promocoes(), linha.escritasDeVolta(),
                    linha.taxaDeAcerto());

        System.out.println();
    }

    private static void exclusivo() {
        System.out.println();
        System.out.println("O QUE O ESTADO EXCLUSIVO ECONOMIZA");
        System.out.println("Carga privada, " + Cargas.ACESSOS + " acessos em todas as linhas,"
                + " variando so quantas linhas cada nucleo usa.");
        System.out.println();

        System.out.printf("  %-18s %10s %14s %14s %20s %18s%n",
                "linhas por nucleo", "linhas", "promocoes MSI", "promocoes MESI",
                "promocoes evitadas", "dos acessos");

        for (Medidas.Exclusivo linha : Medidas.exclusivo())
            System.out.printf(Locale.ROOT, "  %-18d %10d %14d %14d %20d %17.2f%%%n",
                    linha.linhasPorNucleo(), linha.linhas(), linha.promocoesMsi(),
                    linha.promocoesMesi(), linha.promocoesEvitadas(),
                    100 * linha.fracaoDosAcessos());

        System.out.println();
        System.out.println("  A economia e uma transacao por LINHA privada, nao por acesso:"
                + " ela acompanha a coluna das linhas.");
        System.out.println();
    }

    private static void dono() {
        System.out.println();
        System.out.println("O QUE O ESTADO DONO ECONOMIZA");
        System.out.println("Carga de produtor e consumidor: um nucleo escreve, os outros"
                + " leem a mesma linha.");
        System.out.println();

        System.out.printf("  %-16s %10s %20s %20s %16s %14s%n",
                "consumidores", "acessos", "de volta no MESI", "de volta no MOESI",
                "total MESI", "total MOESI");

        for (Medidas.Dono linha : Medidas.dono())
            System.out.printf("  %-16d %10d %20d %20d %16d %14d%n",
                    linha.consumidores(), linha.acessos(), linha.escritasDeVoltaMesi(),
                    linha.escritasDeVoltaMoesi(), linha.totalMesi(), linha.totalMoesi());

        System.out.println();

        for (Medidas.Dono linha : Medidas.dono())
            System.out.printf(Locale.ROOT, "  com %d consumidor(es), o MOESI gasta %.1f%%"
                    + " menos transacoes.%n", linha.consumidores(), 100 * linha.economia());

        System.out.println();
    }

    private static void falso() {
        System.out.println();
        System.out.println("O COMPARTILHAMENTO FALSO E O ENCHIMENTO QUE O CONSERTA");
        System.out.println("As duas cargas tem as MESMAS escritas, na mesma ordem. A"
                + " diferenca e so onde o dado cai:");
        System.out.println("juntos na mesma linha, ou um em cada linha. Nenhum nucleo toca no"
                + " dado do outro em nenhuma.");
        System.out.println();

        System.out.printf("  %-10s %10s %16s %18s %14s %16s %14s%n",
                "nucleos", "acessos", "juntos", "separados", "acerto junto",
                "acerto separado", "vezes pior");

        for (Medidas.Falso linha : Medidas.falso())
            System.out.printf(Locale.ROOT, "  %-10d %10d %16d %18d %14.4f %16.4f %13.0fx%n",
                    linha.nucleos(), linha.acessos(), linha.transacoesJuntos(),
                    linha.transacoesSeparados(), linha.acertoJuntos(),
                    linha.acertoSeparados(), linha.quantasVezesPior());

        System.out.println();
        System.out.println("  A coerencia e mantida por LINHA, e o programa nao tem como"
                + " saber disso sem olhar o endereco.");
        System.out.println();
    }
}
