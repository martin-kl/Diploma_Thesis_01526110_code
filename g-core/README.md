# G-CORE Grammar and Parser
Taken from: [Source](https://github.com/ldbc/ldbc_gcore_parser)

A repository to work towards an open-source grammar for the property graph query language developed by the LDBC Graph Query Language Task Force.  Started on 29 August 2017, as an outcome of an all-hands Task Force meeting in Santiago de Chile earlier in the month.

This repository contains:

 - A grammar/parser for G-CORE ([gcore-spoofax/syntax/](gcore-spoofax/syntax/))
 - Example queries:
   - Queries from the G-CORE paper ([gcore-spoofax-tests/queries-gcore-paper.spt](gcore-spoofax-tests/queries-gcore-paper.spt))
   - LDBC Social Network Benchmark / Interactive ([gcore-spoofax-tests/ldbc-snb-interactive.spt](gcore-spoofax-tests/ldbc-snb-interactive.spt))
   - LDBC Social Network Benchmark / Business Intelligence ([gcore-spoofax-tests/ldbc-snb-bi.spt](gcore-spoofax-tests/ldbc-snb-bi.spt))

## Building the Parser

```bash
export MAVEN_OPTS="-Xms512m -Xmx1024m -Xss16m"
cd gcore-spoofax/
mvn clean install
```
*Note: this worked for me only in Unix (WSL) and not in Windows where this build failed 
caused by a file permission problem on temp files.*

## Running the Unit Tests

```bash
cd gcore-spoofax-tests/
mvn test
```
