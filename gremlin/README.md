Gremlin queries for Master thesis and LDBC SNB
==============================================

This repository contains implementations of the [Linked Data Benchmark
Council](http://www.ldbcouncil.org/)'s [Social Network Benchmark](http://www.ldbcouncil.org/benchmarks/snb) 
for the Titan graph database and further  queries from the diploma thesis of Martin-Kl at TU Wien.

It was forked from [PlatformLab's GitHub](https://github.com/PlatformLab/ldbc-snb-impls), where we removed 
everything that did not regard the Titan database, and added our queries under 
[snb-interactive-tools](snb-interactive-tools/src/main/java/at/tuwien/dbai/thesis/gremlin).
We left the rest of the repository as is such that other users can compare our queries with 
the ones that are fully implemented and fully comply with the LDBC SNB specification.

We adapted the [TitanGraphLoader](snb-interactive-titan/src/main/java/net/ellitron/ldbcsnbimpls/interactive/titan/TitanGraphLoader.java)
such that it works with data produced by version 0.3.2 of the [LDBC datagen](https://github.com/ldbc/ldbc_snb_datagen/releases/tag/v0.3.2).

---

#### Workload Implementations of the interactive workload from PlatformLab:
* Complex Read Queries: 1/14
* Short Read Queries: 7/7
* Update (now insert) Queries: 8/8
