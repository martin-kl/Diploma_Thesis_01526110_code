# Master thesis GSQL queries
Contains some queries from [tigergraph/ecosys GitHub](https://github.com/tigergraph/ecosys/tree/ldbc/ldbc_benchmark/tigergraph).

##Description
This folder contains the GSQL queries used in the Diploma/Master thesis at TU Wien by Martin-Kl in 2020.  
The majority of the queries is from the source mentioned above, I did however change some of them such that:
1. they compile/run on my schema
2. they use the other syntax version that allowed my to show some characteristic in the thesis

## Structure
- We use the schema from [Ecosys GitHub, gsql102/3.0](https://github.com/tigergraph/ecosys/blob/ldbc/ldbc_benchmark/tigergraph/gsql102/3.0/setup_schema.gsql) that can be found under [schema](schema/)
- to load data into the TigerGraph DB, use the scripts under [data_loading](data_loading/)
- [queries](queries/) contains the queries:
    - `ic1.gsql` - `ic14.sql` and ``is1.gsql`` - ``is7.sql`` are taken from the above mentioned repository
    - the other queries are added by me

## Install the queries
The queries need to be installed before they can be run.
I did this using GraphStudio UI.


## Running the Queries
Installed queries can be either run over the UI or in the GSQL shell via
```
RUN QUERY <query_name> (<parameters>)
```
