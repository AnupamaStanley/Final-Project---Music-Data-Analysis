package MusicDataProject.DataAnalysis

import org.apache.spark.sql.SparkSession

object DataAnalysis {
  def main(args: Array[String]): Unit = {
     val sparkSession = SparkSession.builder.master("local")
                     .appName("spark session example")
                     .config("spark.sql.warehouse.dir","/user/hive/warehouse")
                     .config("hive.metastore.uris", "thrift://localhost:9083")
                     .enableHiveSupport().getOrCreate()
    val batchId = args(0)


    val create_top_10_stations = """CREATE TABLE IF NOT EXISTS top_10_stations(
        station_id STRING, total_distinct_songs_played INT, distinct_user_count INT )
        PARTITIONED BY (batchid INT) ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE"""

    val load_top_10_stations = s"""INSERT OVERWRITE TABLE top_10_stations PARTITION(batchid='$batchId')
          SELECT station_id, COUNT(DISTINCT song_id) AS total_distinct_songs_played, COUNT
          (DISTINCT user_id) AS distinct_user_count FROM enriched_data WHERE status='pass'
          AND batchid='$batchId' AND like=1 GROUP BY station_id ORDER BY total_distinct_songs_played
          DESC LIMIT 10"""


    val create_users_behaviour = """CREATE TABLE IF NOT EXISTS users_behaviour( user_type STRING, duration INT )
        PARTITIONED BY (batchid INT) ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE"""

    val load_users_behaviour = s"""INSERT OVERWRITE TABLE users_behaviour PARTITION(batchid='$batchId')
        SELECT CASE WHEN (su.user_id IS NULL OR CAST(ed.timestamp AS DECIMAL(20,0)) > CAST(su.subscn_end_dt AS DECIMAL(20,0)))
        THEN 'UNSUBSCRIBED' WHEN (su.user_id IS NOT NULL AND CAST(ed.timestamp AS DECIMAL(20,0)) <= CAST(su.subscn_end_dt
        AS DECIMAL(20,0))) THEN 'SUBSCRIBED' END AS user_type, SUM(ABS(CAST(ed.end_ts AS DECIMAL(20,0))-CAST(ed.start_ts AS DECIMAL(20,0))))
        AS duration FROM enriched_data ed LEFT OUTER JOIN subscribed_users su ON ed.user_id=su.user_id WHERE ed.status='pass' AND
        ed.batchid='$batchId' GROUP BY CASE WHEN (su.user_id IS NULL OR CAST(ed.timestamp AS DECIMAL(20,0)) > CAST(su.subscn_end_dt
        AS DECIMAL(20,0))) THEN 'UNSUBSCRIBED' WHEN (su.user_id IS NOT NULL AND CAST(ed.timestamp AS DECIMAL(20,0)) <= CAST
        (su.subscn_end_dt AS DECIMAL(20,0))) THEN 'SUBSCRIBED' END"""


    val create_connected_artists = """CREATE TABLE IF NOT EXISTS connected_artists( artist_id STRING, user_count INT )
        PARTITIONED BY (batchid INT) ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE"""

    val load_connected_artists = s"""INSERT OVERWRITE TABLE connected_artists PARTITION(batchid='$batchId')
        SELECT ua.artist_id, COUNT(DISTINCT ua.user_id) AS user_count FROM ( SELECT user_id, artist_id FROM users_artists
        LATERAL VIEW explode(artists_array) artists AS artist_id ) ua INNER JOIN ( SELECT artist_id, song_id, user_id FROM enriched_data
        WHERE status='pass' AND batchid='$batchId' ) ed ON ua.artist_id=ed.artist_id AND ua.user_id=ed.user_id GROUP BY ua.artist_id
        ORDER BY user_count DESC LIMIT 10"""



    val create_top_10_royalty_songs = """CREATE TABLE IF NOT EXISTS top_10_royalty_songs( song_id STRING, duration INT )
        PARTITIONED BY (batchid INT) ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE"""

    val load_top_10_royalty_songs = s"""INSERT OVERWRITE TABLE top_10_royalty_songs PARTITION(batchid='$batchId')
        SELECT song_id, SUM(ABS(CAST(end_ts AS DECIMAL(20,0))-CAST(start_ts AS DECIMAL(20,0)))) AS duration
        FROM enriched_data WHERE status='pass' AND batchid='$batchId' AND (like=1 OR song_end_type=0) GROUP BY song_id
        ORDER BY duration DESC LIMIT 10"""



    val create_top_10_unsubscribed_users = """CREATE TABLE IF NOT EXISTS top_10_unsubscribed_users( user_id STRING, duration INT )
        PARTITIONED BY (batchid INT) ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE"""

    val load_top_10_unsubscribed_users = s"""INSERT OVERWRITE TABLE top_10_unsubscribed_users PARTITION(batchid='$batchId')
        SELECT ed.user_id, SUM(ABS(CAST(ed.end_ts AS DECIMAL(20,0))-CAST(ed.start_ts AS DECIMAL(20,0)))) AS duration
        FROM enriched_data ed LEFT OUTER JOIN subscribed_users su ON ed.user_id=su.user_id WHERE ed.status='pass'
        AND ed.batchid='$batchId' AND (su.user_id IS NULL OR (CAST(ed.timestamp AS DECIMAL(20,0)) > CAST(su.subscn_end_dt AS DECIMAL(20,0))))
        GROUP BY ed.user_id ORDER BY duration DESC LIMIT 10"""


    try {
      sparkSession.sqlContext.sql("SET hive.auto.convert.join=false")
      sparkSession.sqlContext.sql("USE project")
      sparkSession.sqlContext.sql(create_top_10_stations)
      sparkSession.sqlContext.sql(load_top_10_stations)
      sparkSession.sqlContext.sql(create_users_behaviour)
      sparkSession.sqlContext.sql(load_users_behaviour)
      sparkSession.sqlContext.sql(create_connected_artists)
      sparkSession.sqlContext.sql(load_connected_artists)
      sparkSession.sqlContext.sql(create_top_10_royalty_songs)
      sparkSession.sqlContext.sql(load_top_10_royalty_songs)
      sparkSession.sqlContext.sql(create_top_10_unsubscribed_users)
      sparkSession.sqlContext.sql(load_top_10_unsubscribed_users)

      sparkSession.sqlContext.sql("SELECT station_id FROM top_10_stations").show()


      sparkSession.sqlContext.sql("SELECT user_type,duration FROM users_behaviour").show()



      sparkSession.sqlContext.sql("SELECT artist_id FROM connected_artists").show()



      sparkSession.sqlContext.sql("SELECT song_id FROM top_10_royalty_songs").show()



      sparkSession.sqlContext.sql("SELECT user_id FROM top_10_unsubscribed_users ").show()

    }
    catch{
      case e: Exception=>e.printStackTrace()
    }
  }
}

