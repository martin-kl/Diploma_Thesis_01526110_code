#!/bin/bash

###############################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for training purpose.
# Author: mingxi.wu@tigergraph.com
################################################

### change to raw data file folder
export LDBC_SNB_DATA_DIR=/home/tigergraph/Desktop/social_network/
export LDBC_SNB_DATA_POSTFIX=_0_0.csv

# define schema and loading job
# gadmin start
gsql setup_schema.gsql

# load data into TigerGraph
# note: we use a different load file than the one in the gsql102 folder
# cause our datagen generated the csv files in static and dynamic folders
./load_data.sh
