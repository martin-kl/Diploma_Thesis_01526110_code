# PGQL Grammar, Parser & Queries

This folder contains the PGQL queries for the Diploma/Master at TU Wien by Martin-Kl in 2020.

## Structure
[Queries.java](src/main/java/at/tuwien/dbai/thesis/pgql/Queries.java) contains the queries used in the thesis.

The parser is added via the Maven dependency.

## Run the queries
Run the following command while located in the pgql folder:
```bash
mvn compile exec:java -Dexec.mainClass="at.tuwien.dbai.thesis.pgql.Queries"
```

