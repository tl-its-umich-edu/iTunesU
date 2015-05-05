This folder is to store useful standalone programs to access/modify iTunesU tool data.

--------------------------------------------------------------------------------------------------------------------------------
Part 1. Useful database queries to find out Sakai sites with iTunesU tool and the site instructors:
--------------------------------------------------------------------------------------------------------------------------------

1. Get all site ids contains itunesu tool: > siteIds.txt

SELECT t1.site_id
FROM
SAKAI_SITE_TOOL t1, sakai_site t2
where 
t2.site_id != '!admin'  
and t1.REGISTRATION='sakai.iTunesU' 
and t1.SITE_ID = t2.SITE_ID
order by t1.site_id

2. Get all site ids contains itunesu and is created more than one years ago: > sites_created_1yearold.txt

SELECT t2.site_id, t2.title
FROM
SAKAI_SITE_TOOL t1, sakai_site t2
where 
t2.site_id != '!admin'  
and t1.REGISTRATION='sakai.iTunesU' 
and t1.SITE_ID = t2.SITE_ID
and t2.createdon < to_date('05/01/11 00:00:00','mm/dd/yy hh24:mi:ss')
order by t1.site_id

3. Get all users with Owner or Instructor role for all sites with iTunesU tool. The result contains three columns: user eid, site title and site id. Save the result as "itunesu_contact.txt"

SELECT
   t5.eid, t2.title, t1.site_id
FROM
SAKAI_SITE_TOOL t1, sakai_site t2, SAKAI_REALM t3, SAKAI_REALM_RL_GR t4, SAKAI_USER_ID_MAP t5, SAKAI_REALM_ROLE t6
where 
t1.REGISTRATION='sakai.iTunesU'
and t1.SITE_ID = t2.SITE_ID
and t2.createdon < to_date('05/01/11 00:00:00','mm/dd/yy hh24:mi:ss')
and '/site/' || t1.site_id = t3.realm_id
and t3.realm_key=t4.realm_key
and (t6.role_name='Instructor' or t6.role_name='Owner')
and t4.role_key = t6.role_key
and t4.user_id = t5.user_id
order by t5.eid, t1.site_id




--------------------------------------------------------------------------------------------------------------------------------
Part II. Java commands to download iTunesU tool content or remove iTunesU tools:
--------------------------------------------------------------------------------------------------------------------------------

1. DownloadITunesuFile.java downloads itunesu content to local drive, based on given list of site ids. Before use, one need to change the program for required credentials and server information. 

java -classpath . DownloadITunesuFile site_id_list_file download_folder_path


2. RemoveITunesUCourses.java remove the itunesu course from iTunesu server, based on a given list of site ids. Before use, one need to change the program for required credentials and server information. 

 java -classpath . RemoveITunesUCourses site_id_list_file

3. ReadITunesU.java parses the iTunes U showTree file, based on a list of sakai site ids. The result is a tab-spearated text file, showing the total file storage size used by each site page on iTunes U.

java -classpath . ReadITunesU showtree_file site_id_list_file > itunesu_storage_file