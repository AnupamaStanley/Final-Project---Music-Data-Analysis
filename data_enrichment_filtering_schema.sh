#!/bin/bash

batchid=`cat /home/acadgild/MusicData/logs/current-batch.txt`
LOGFILE=/home/acadgild/MusicData/logs/log_batch_$batchid

echo "Creating hive tables on top of hbase tables for data enrichment and filtering..." >> $LOGFILE

hive -f /home/acadgild/MusicData/files/create_hive_hbase_lookup.hql

