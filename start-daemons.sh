#!/bin/bash

if [ -f "/home/acadgild/MusicData/logs/current-batch.txt" ]
then
 echo "Batch File Found!"
else
 echo -n "1" > "/home/acadgild/MusicData/logs/current-batch.txt"
fi

chmod 775 /home/acadgild/MusicData/logs/current-batch.txt
batchid=cat `/home/acadgild/MusicData/logs/current-batch.txt`
LOGFILE=/home/acadgild/MusicData/logs/log_batch_$batchid

echo "Starting daemons" >> $LOGFILE

start-all.sh
start-hbase.sh

