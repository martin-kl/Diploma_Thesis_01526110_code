# G-CORE Grammar, Parser & Queries
Forked from: [Source](https://github.com/ldbc/ldbc_gcore_parser) and extended with the G-CORE queries for the 
Diploma/Master at TU Wien by Martin-Kl in 2020.

##Description
A repository to work towards an open-source grammar for the property graph query language developed by the LDBC Graph 
Query Language Task Force.

Extended/adapted by Martin-Kl to include the G-CORE queries used in the thesis.

## Structure
 - A grammar/parser for G-CORE ([gcore-spoofax/syntax/](gcore-spoofax/syntax/))
 - Example queries:
   - Queries from the G-CORE paper ([gcore-spoofax-tests/queries-gcore-paper.spt](gcore-spoofax-tests/queries-gcore-paper.spt))
   - LDBC Social Network Benchmark / Interactive complex queries ([gcore-spoofax-tests/ldbc-snb-interactive.spt](gcore-spoofax-tests/ldbc-snb-interactive-complex.spt))
   - LDBC Social Network Benchmark / Business Intelligence ([gcore-spoofax-tests/ldbc-snb-bi.spt](gcore-spoofax-tests/ldbc-snb-bi.spt))
 - queries added by Martin:
   - Queries occurring in the thesis, including some LDBC SNB Interactive short queries [gcore-spoofax-tests/thesis-queries.spt](gcore-spoofax-tests/thesis-queries.spt)

## Building the Parser

```bash
export MAVEN_OPTS="-Xms512m -Xmx1024m -Xss16m"
cd gcore-spoofax/
mvn clean install
```
*Note from Martin: this worked for me only in Unix (WSL) and not in Windows where this build failed 
caused by a file permission problem on temp files.*

## Running the Unit Tests

```bash
cd gcore-spoofax-tests/
mvn test
```
