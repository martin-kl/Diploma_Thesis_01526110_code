## Neo4j
- neo4j-community-4.1.0
- needs java >= 11
- start with ``.\neo4j.ps1 start`` from windows

## TigerGraph:
- used version: TigerGraph 3.0 Developer Edition in Oracle VM VirtualBox
- start: run virtualBox image, ``gadmin start all``

<!-- 
Drag- Drop zu Tigergraph aktivieren:
guest additions einlegen + installieren, danach folgende commands:
sudo apt-get install virtualbox-guest-dkms 
sudo apt-get install virtualbox-guest-utils
sudo apt-get install virtualbox-ext-pack
danach restart, danach Drag & Drop aktivieren
-->

## Titan with Gremlin (Tinkerpop)
- version: titan-1.0.0-hadoop1.zip, requires java 8 (jdk)
- run these commands in WSL in folder ``titan-1.0.0-hadoop1/bin``
- remove everything if needed: ``./titan.sh stop && ./titan.sh clean && ./titan.sh start``
- or just start titan with default cassandra storage backend (everything in WSL): ``./titan.sh start``
- start gremlin: ``./gremlin.sh``

<!--
note on titan:
- connect to sample graph on cassandra: http://s3.thinkaurelius.com/docs/titan/1.0.0/getting-started.html#_loading_the_graph_of_the_gods_into_titan:
- ``graph = TitanFactory.open('conf/titan-berkeleyje-es.properties')``
- ``GraphOfTheGodsFactory.load(graph)``
- ``g = graph.traversal()`` and work with g like ``g.V()``
-->

---



# regarding LDBC:
Source: https://github.com/ldbc/ldbc_snb_datagen/
- downloaded everything from the git repo, open docker_run.sh and save it with LF instead of CR LF  
- configure params.ini, for example to generate only small amount of data with "snb.interactive.0.1" (scaleFactor)

- then build the image via (needs quite some time as this image has ~2GB)
```
docker build . --tag ldbc/datagen
```

- run following commands from .../ldbc_snb_datagen/ folder:
- remove previous folders (run in bash):
```
rm -rf social_network/ substitution_parameters
```

- and run docker in normal cmd/powershell via
```
docker run --rm --mount type=bind,source="$(pwd)/",target="/opt/ldbc_snb_datagen/out" --mount type=bind,source="$(pwd)/params.ini",target="/opt/ldbc_snb_datagen/params.ini" ldbc/datagen
```

- run this in bash:
```
    sudo chown -R $USER:$USER social_network/ substitution_parameters/
```



---



# Importing data

## Neo4j
1. run convert-csvs.sh in bash (**Attention**: files are then *not* usable to e.g. be imported into TigerGraph)
    - first:
    ```
    export NEO4J_DATA_DIR=/mnt/d/University/diploma_thesis/ldbc/ldbc_snb_datagen/social_network
    export POSTFIX=_0_0.csv
    ```
    - then run script
2. edit neo4j.conf file:
    - go to ``\diploma_thesis\neo4j-community-4.1.0\conf\neo4j.conf``
    - comment out the following line to allow imports from other directories:
    ``#dbms.directories.import=import``
3. import
    - first stop db
    - remove neo4j folder (DB) under 
        - ``neo4j-community-4.1.0\data\databases\``
        - and ``neo4j-community-4.1.0\data\transactions\``
    - ensure that following Environment variables are set:
        ``NEO4J_DATA_DIR=D:\University\diploma_thesis\ldbc\ldbc_snb_datagen\social_network``  
        ``POSTFIX=_0_0.csv``  
        *Note that after changing one of these variables we have to restart e..g ConEmu*
    - run the adapted ``import-to-neo4j.ps1`` file that imports everything to the "neo4j" database (does not have to exist beforehand)
    - start neo4j


######################################################
## Import data into TigerGraph
- download ecosys from github, Note: this is too big to clone via git -> google for svn method to only download the ldbc_benchmark/tigergraph directory
- either generate data in virtual machine or simply copy files in there 
    - (enable Drag & Drop before, see TigerGraph; Note: drag & Drop files from each folder, subfolder does not work automatically)
    - import the files as generated, i.e. not after the headers are changed from the Neo4j import script
### Load schema and data (files checked into git)
- note: we use the schema from ``ldbc/ldbc_benchmark/tigergraph/gsql102/3.0/setup_schema.gsql``
- import everything at once: schema & data by invoking ``./one_step_load.sh``
- manually:
    - import schema: ``gsql setup_schema.gsql``
    - check path to files in ``load_data.sh``, e.g.: ``LDBC_SNB_DATA_DIR=/home/tigergraph/Desktop/social_network/`` <!--do not forget: export LDBC_SNB_DATA_POSTFIX=_0_0.csv-->
    - (start tigergraph (``gadmin start all``))
    - import data ``./load_data.sh``
<!-- [note from before:] **ATTENTION**: simply running ``./one_step_load.sh`` did not work, I think cause it's too much data (times out) even on scale 0.1 -> import in parts: (runs quite long, potentially caused by index creation)
- start with schema creation/file ``gsql setup_schema.gsql`` (also look into the one_step_load file for env. variables DATA_DIR and POSTFIX)
- adopt load_data.sh to not import everything at once but I did it in 3 parts -> comment out rest and leave only parts
-->

### Install the Queries
- **Attention:** I took the queries from the ecosys git - but not from the main directory but from: ``ldbc_benchmark/tigergraph/queries_linear/queries`` <!--note that this folder does not exist any more! -> 
(this needs the schema of ``ldbc/ldbc_benchmark/tigergraph/gsql102/3.0/setup_schema.gsql``) 
    - query is7 is changed to work with our schema, other queries taken from there
- run installation script ``./install_queries.sh``


######################################################
## Import into Titan (with Tinkerpop/Gremlin)

<!--
### with code from graph-benchmarking-master/snb-interactive-gremlin (anilpacaci on github)
- as Titan is running in WSL, also import there (also does not work in Windows)
- first install ldbc driver in WSL -> via ``mvn install -DskipTests``
- go to repository that contains the loading files and everything: https://github.com/anilpacaci/graph-benchmarking/tree/master/snb-interactive-gremlin
- adopt ``initTitan.groovy``'s initializeTitan method: change ``String propertiesFile`` to ``BaseConfiguration conf`` and use of conf variable
- then install the files in WSL -> via ``mvn install`` in snb-interactive-gremlin directory
- start titan, then start gremlin shell ``./gremlin.sh``

now start loading everything (everything in gremlin shell):
- ``:load ../../gremlin/graph-benchmarking-master/snb-interactive-gremlin/scripts/SNBParser.groovy``
- ``:load ../../gremlin/graph-benchmarking-master/snb-interactive-gremlin/scripts/initTitan.groovy``
- create config:
    - config = new BaseConfiguration()
    - config.setProperty("storage.backend", "cassandra")
    - config.setProperty("storage.hostname", "127.0.0.1")
    - config.setProperty("storage.cassandra.keyspace", "snb")
- call it: ``graph = initializeTitan(config)``
- import in some way like: `` SNBParser.loadSNBGraph(graph, SNB_SOCIAL_NETWORK, 100, 1000)`` **but this does not work yet!**

---
-->

### with code from PlatformLab, ldbc-snb-impls folder
- start titan in WSL
- load data into Titan:
    - as this is based on older ldbc datagen, copy all csv files into a single folder (does not matter which one)
    - use Java 8 und execute TitanGraphLoader with these arguments (did it in Intellij)  
  ``-C 127.0.0.1 -input D:\University\diploma_thesis\ldbc\ldbc_snb_datagen\social_network_titan -batchSize 512 -graphName default -progReportPeriod 10`` (Attention: runs quite long, >20 mins)
- queries can be run with included QueryTester.java:
    - adapt params like ``shortquery1 933`` that runs and outputs result of shortquery1 for person with id 933

<!--
### with code from https://bitbucket.org/dbtrentogdb/ldbc_gen/src/master/ into TinkerGraph (in memory db, NOT titan)
*gave up and switched over to titan*  
the plan there is to use one docker file that on docker build uses the snb datagen to create some data and on docker run loads this into gremlin
- until now I managed to adapt the dockerfile such that it builds -> also seems to generate some data
- but on import it imports 0 edges and 0 nodes -> I think the data is never exported correctly to the real file system and stays somewhere in the container
- could import data by:
    - generating it from the snb_datagen manually
    - copying it to the runtime/data dir (only csv files)
    - running ``docker run  -v D:\University\diploma_thesis\gremlin\LDBC_Gen_from_dbtrentogdb\runtime\:\runtime -e JAVA_OPTIONS='-Xms1G -Xmn128M -Xmx4G' gremlin/ldbc_gen``
    - however, I never manage to connect to this in-memory DB then, so give it up and try to use TITAN !!!!!!


*note on debugging docker builds*: call ``docker run -ti --rm 7d91 bash`` to start a shell in the image 7d91
-->




---
# Run Queries:

## Cypher
- copy queries from ex. impls to online Qu. editor (http://localhost:7474/browser/) and run it there

impls:
- https://github.com/ldbc/ldbc_snb_implementations
    - incl. updates on dev: https://github.com/ldbc/ldbc_snb_implementations/tree/dev/cypher/queries
- https://github.com/tigergraph/ecosys/blob/ldbc/ldbc_benchmark/neo4j/query/query_runner.py

---
## Gremlin
Queries run on Titan DB, so first start it in WSL ``./titan.sh start``
- run queries with QueryTester.java from ldbc-snb-impls:
    - adapt params like ``shortquery1 933`` that runs and outputs result of shortquery1 for person with id 933
- manually run queries in gremlin shell:
    - do all below in one step by loading script: ``./gremlin.sh startup.grooxy`` <!-- note: the script simply contains the two lines graph=... and g=graph.... -->
    - or do it manually:
        - start gremlin: ``./gremlin.sh``
        - connect to cassandra:  
        ``graph = TitanFactory.open('../../Diploma_Thesis_01526110_code/gremlin/titan-cassandra.properties')``
        - create graph traversal: ``g = graph.traversal()``
        - run queries on g


---
## PGQL
no database, queries syntactically checked with [Parser from Oracle](https://github.com/oracle/pgql-lang)


---
## GSQL
Tigergraph in VirtualBox, queries are already installed  
- use GraphStudio: http://localhost:14240/ to access and run the queries, login with tigergraph:tigergraph and change to ldbc graph
- write new queries there, save them and either:
    - run in interpreted mode (slower)
    - or install them (optimizes the query) and run it installed, this can also be done over command line

---
## G-CORE
Files taken from: https://github.com/ldbc/ldbc_gcore_parser, includes the parser and the implementation of the LDBC SNB and BI queries under`` gcore-spoofax-tests/``

- maven installation failed in Windows -> run it in WSL as given:
    - install spoofax (
        ``export MAVEN_OPTS="-Xms512m -Xmx1024m -Xss16m"``, 
        ``cd gcore-spoofax/``, 
        ``mvn clean install``)
    - ``cd gcore-spoofax-tests/``
    - ``mvn test``