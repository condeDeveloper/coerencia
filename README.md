# coerencia

Coerência de cache do zero em Java 21: **o mesmo programa custa 1.018 vezes mais transações
quando dois dados cabem na mesma linha.**

MSI, MESI e MOESI implementados inteiros, com quatro núcleos, um cache por núcleo e um
barramento que conta o que atravessa.

## Dois juízes, e nenhum deles é um protocolo

O primeiro é uma **memória sem cache nenhum**: um vetor. Os acessos acontecem um depois do
outro, na ordem da carga, e o que uma leitura tem de devolver é o último valor escrito naquela
posição. Não há estado, não há barramento, não há invalidação.

O segundo é a **invariante da coerência**, conferida depois de cada acesso e não só no fim: no
máximo um cache com a linha em estado de escrita; se há um modificado, mais ninguém tem cópia
válida; e toda cópia limpa é igual à da memória.

Conferir só no fim esconderia o que interessa. Um protocolo pode acertar todos os valores e
passar por estados impossíveis no caminho, e é desses estados que sairia o erro com qualquer
carga um pouco diferente.

```
$ mvn exec:java -Dexec.args="correcao"

  protocolo carga                       acessos   leituras  valores errados  estados invalidos
  MESI     privada                         4000       2000                0                  0
  MESI     so leitura                      4000       4000                0                  0
  MESI     migratoria                      4000       2000                0                  0
  MESI     produtor e consumidor           4000       3000                0                  0
  MESI     compartilhamento falso          4000          0                0                  0
  MESI     com enchimento                  4000          0                0                  0

  protocolo     cargas      acessos   cargas erradas
  MSI               50       100000                0
  MESI              50       100000                0
  MOESI             50       100000                0
```

## A diferença entre os três cabe em dois bits

Os três são de invalidação: quem vai escrever manda os outros jogarem fora a cópia. O que os
separa são duas perguntas.

**Existe o estado EXCLUSIVO?** Com ele, o cache que leu um bloco que ninguém mais tem sabe
disso, e a escrita seguinte não precisa avisar ninguém.

**Existe o estado DONO?** Com ele, o cache que tem a linha suja pode entregá-la a outro e
continuar responsável por ela, sem escrever de volta na memória.

```
$ mvn exec:java -Dexec.args="transacoes"

  protocolo carga                      por acesso    leitura  exclusiva   promocao   de volta     acerto
  MSI      privada                         0.0480         64          0         64         64     0.9840
  MSI      so leitura                      0.0640        256          0          0          0     0.9360
  MSI      migratoria                      1.1445       1526          0       1526       1526     0.6185
  MSI      produtor e consumidor           1.2500       3000         64        936       1000     0.2340
  MSI      compartilhamento falso          1.4880          0       2976          0       2976     0.2560
  MESI     privada                         0.0320         64          0          0         64     0.9840
  MESI     migratoria                      1.1285       1526          0       1462       1526     0.6185
  MESI     produtor e consumidor           1.2500       3000         64        936       1000     0.2340
  MOESI    produtor e consumidor           1.0160       3000         64        936         64     0.2340
  MOESI    compartilhamento falso          1.4880          0       2976          0       2976     0.2560
```

Cada bit serve para **uma** carga. O exclusivo só muda a privada; o dono só muda o produtor e
consumidor. Na migratória, que é o pior caso de todos, nenhum dos dois ajuda; no
compartilhamento falso, os três gastam exatamente o mesmo.

## O estado exclusivo economiza por linha, não por acesso

```
$ mvn exec:java -Dexec.args="exclusivo"

  linhas por nucleo      linhas  promocoes MSI promocoes MESI   promocoes evitadas        dos acessos
  1                           4              4              0                    4              0.10%
  2                           8              8              0                    8              0.20%
  4                          16             16              0                   16              0.40%
  8                          32             32              0                   32              0.80%
  16                         64             64              0                   64              1.60%
```

O número de acessos é o mesmo nas cinco linhas: 4.000. O que muda é quantas linhas privadas
cada núcleo usa, e as promoções evitadas são **exatamente** o número de linhas.

Isso diz onde o ganho mora. Dobrar os acessos sobre os mesmos dados não economiza nada a mais;
dobrar os dados, sim. Em compensação, como quase não há transação nenhuma numa carga privada,
esse punhado de promoções é **um terço de tudo o que atravessa o barramento**, em todas as
cinco linhas.

## O estado dono economiza a escrita de volta

```
$ mvn exec:java -Dexec.args="dono"

  consumidores        acessos     de volta no MESI    de volta no MOESI       total MESI    total MOESI
  1                      4000                 2000                   64             6000           4064
  2                      3999                 1333                   64             5332           4063
  3                      4000                 1000                   64             5000           4064
```

Com um consumidor só, o MOESI corta 32,3% das transações; com três, 18,7%. As escritas de volta
evitadas são uma por produção, e as leituras dos consumidores entram no total sem mudar isso:
quanto mais gente lê cada valor, menos a economia pesa.

As 64 escritas de volta que sobram no MOESI são as do esvaziamento final, uma por linha.

## O compartilhamento falso

```
$ mvn exec:java -Dexec.args="falso"

  nucleos       acessos           juntos          separados   acerto junto  acerto separado     vezes pior
  2                4000             4070                  4         0.4913           0.9995          1018x
  3                4000             5370                  6         0.3288           0.9993           895x
  4                4000             5952                  8         0.2560           0.9990           744x
```

As duas cargas comparadas têm **exatamente as mesmas escritas**, na mesma ordem, na mesma
quantidade de dados. Nenhum núcleo lê ou escreve o dado do outro em nenhuma das duas. Para o
programa, elas são idênticas.

A única diferença é onde o dado cai: juntos na mesma linha de cache, ou um em cada linha. A
coerência é mantida **por linha**, e o hardware não tem como saber que a disputa não existe.

E a razão cai conforme entram núcleos, de 1.018 para 744, o que é o contrário do que parece:
com mais núcleos, o custo de quem está separado sobe por núcleo, e o de quem está junto sobe
menos que proporcionalmente.

## Como rodar

```
mvn test
mvn exec:java -Dexec.args="tudo"
```

Os comandos do medidor são `correcao`, `transacoes`, `exclusivo`, `dono` e `falso`.

## O que as medidas me corrigiram

**Eu ia escrever que o estado exclusivo economiza uma transação por escrita. Ele economiza uma
por LINHA.** Com 4.000 acessos sobre 4 linhas privadas ele evita 4 promoções; sobre 64 linhas,
evita 64. Depois da primeira escrita, a linha fica em modificado e as escritas seguintes já não
custavam nada no MSI também. O ganho vale em todo lugar e é proporcional ao tamanho dos dados,
não ao trabalho feito sobre eles.

**E mesmo assim ele é um terço do barramento.** Os dois números parecem brigar: 1,6% dos
acessos e 33% das transações. Não brigam, e a razão é que uma carga privada quase não usa o
barramento. Medir economia como fração dos acessos esconde isso; medir como fração do que
atravessa o barramento, que é o recurso disputado, mostra.

**Achei que o MOESI ganhasse mais quanto mais gente lesse.** É o contrário: 32,3% com um
consumidor, 18,7% com três. Ele economiza uma escrita de volta por produção, e esse número não
muda; o que cresce é o total, por causa das leituras.

**Na carga migratória, que é a que mais dói, nenhum dos dois bits serve.** São 1,1445
transações por acesso no MSI e 1,1285 no MESI, e o MOESI é igual ao MESI. O que o protocolo faz
de melhor não tem nada a ver com o pior caso dele, e escolher protocolo por catálogo de estados
é escolher pelo que menos importa.

**E a maior diferença medida aqui não é entre protocolos.** É entre dois programas idênticos
com endereços diferentes: 4.070 transações contra 4. Nenhuma escolha de protocolo chega perto
de um fator de mil.

## O que não tem aqui

**Capacidade.** O cache daqui é infinito: uma linha só sai dele quando outro núcleo manda sair.
Isso separa o custo de coerência do custo de capacidade, que é o assunto de
[`despejo`](https://github.com/condeDeveloper/despejo).

**Atualizar em vez de invalidar.** Existe a família que manda o valor novo para quem tem cópia,
em vez de mandar jogar fora. Ela ganha na carga de produtor e consumidor e perde feio quando
alguém escreve muitas vezes seguidas sem ninguém ler.

**Diretório.** Com dezenas de núcleos não há barramento que todo mundo escute. O diretório
guarda quem tem cada linha e fala só com esses, e a conta de transações muda inteira.

**A ordem das escritas.** Aqui os acessos acontecem um depois do outro, e coerência é sobre uma
posição de cada vez. O que um núcleo pode ver da ORDEM das escritas de outro é consistência de
memória, que é outro assunto e muito mais escorregadio.

**Tempo.** A unidade aqui é a transação, nunca o segundo. Quanto custa cada uma depende do
barramento, da distância entre os núcleos e do que mais estiver passando, e nada disso está
modelado.

## Licença

MIT.
