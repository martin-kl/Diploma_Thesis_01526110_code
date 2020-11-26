# Master thesis Gremlin queries

This repository contains Gremlin implementations of the [Linked Data Benchmark
Council](http://www.ldbcouncil.org/) 's [Social Network Benchmark](http://www.ldbcouncil.org/benchmarks/snb) 
for the Titan graph database and further queries from the diploma thesis of Martin-Kl at TU Wien.

It was forked from [PlatformLab's GitHub](https://github.com/PlatformLab/ldbc-snb-impls), where I first removed 
everything that did not regard the Titan database.

The added queries can be found under 
[snb-interactive-tools](snb-interactive-tools/src/main/java/at/tuwien/dbai/thesis/gremlin).
The rest of the repository is still kept unchanged so that other users can compare our queries with 
the ones that are fully implemented and fully comply with the LDBC SNB specification.

I furthermore adapted the [TitanGraphLoader](snb-interactive-titan/src/main/java/net/ellitron/ldbcsnbimpls/interactive/titan/TitanGraphLoader.java)
such that it works with data produced by version 0.3.2 of the [LDBC datagen](https://github.com/ldbc/ldbc_snb_datagen/releases/tag/v0.3.2).

---

### Running the queries
Configure the connection parameters for the Titan database by passing the desired properties to the constructor of 
[TitanDbConnectionState.java](snb-interactive-titan/src/main/java/net/ellitron/ldbcsnbimpls/interactive/titan/TitanDbConnectionState.java)  
in [Queries.java](snb-interactive-tools/src/main/java/at/tuwien/dbai/thesis/gremlin/Queries.java).

Run the queries using the class [Queries.java](snb-interactive-tools/src/main/java/at/tuwien/dbai/thesis/gremlin/Queries.java).

---

#### Workload Implementations of the interactive workload from PlatformLab:
* Complex Read Queries: 1/14 - IC1 is implemented by PlatformLab, I added a partial implementation of IC13
* Short Read Queries: 7/7
* Update (now insert) Queries: 8/8
