# Master/Diploma thesis by Martin-Kl
- Title: **Analysis and Comparison of Common Graph Query Languages**
- Organization: TU Wien
- Faculty: Informatics
- Year: 2020

## Description
This repository contains the code files/queries used in my master thesis.  
Not all queries in this repository are implemented by me, for files/queries taken from other repositories see
the corresponding README files in the sub-directories.


## Technologies, Databases & versions
### Cypher ([README](cypher/README.md))
- Neo4j community edition 4.1.0
- requires Java >= 11
### Gremlin ([README](gremlin/README.md))
- [Titan 1.0.0 hadoop1](https://github.com/thinkaurelius/titan/)
- requires Java 8
### PGQL ([README](pgql/README.md))
- Parser from [here](https://github.com/oracle/pgql-lang), on current version of PGQL: 1.3
### GSQL ([README](gsql/README.md))
- TigerGraph 3.0 Developer Edition
### G-CORE ([README](g-core/README.md))
- Parser from [here](https://github.com/ldbc/ldbc_gcore_parser)
### LDBC Datagen
- I used version 0.3.2 of the data generator to generate the test data that was then imported into Neo4j (Cypher),
Titan (Gremlin) and TigerGraph (GSQL)
 - [LDBC datagen 0.3.2](https://github.com/ldbc/ldbc_snb_datagen/releases/tag/v0.3.2)


## Structure
Extended notes on how I ran everything can be found [here](Notes%20on%20how%20to%20run%20everything.md).  
Each sub-folder has its own README file that contains more information on 