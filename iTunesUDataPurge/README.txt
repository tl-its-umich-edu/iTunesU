This folder is to store useful standalone programs to access/modify iTunesU tool data.

Java commands to download iTunesU tool content or remove iTunesU tools:

1. DownloadITunesuFile.java downloads itunesu content to local drive, based on given list of site ids. Before use, one need to change the program for required credentials and server information. 

java -classpath . DownloadITunesuFile site_id_list_file download_folder_path


2. RemoveITunesUCourses.java remove the itunesu course from iTunesu server, based on a given list of site ids. Before use, one need to change the program for required credentials and server information. 

 java -classpath . RemoveITunesUCourses site_id_list_file

3. ReadITunesU.java parses the iTunes U showTree file, based on a list of sakai site ids. The result is a tab-spearated text file, showing the total file storage size used by each site page on iTunes U.

java -classpath . ReadITunesU showtree_file site_id_list_file > itunesu_storage_file
