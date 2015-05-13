This folder is to store useful standalone programs to access/modify iTunesU tool data.

To compile and build executables, do:

mvn package

It will generate jar file iTunesuDataPurge-<VERSION>-jar-with-dependencies.jar in the target folder.

Java commands to download iTunesU tool content or remove iTunesU tools:

1. ReadITunesU.java parses the iTunes U showTree file, based on a list of sakai site ids. 

It accepts one parameter, site_id.txt, with line format of <site_name>/t<site_id>

The result is a tab-spearated text file, showing the total file storage size used by each site page on iTunes U.

java -cp target/iTunesuDataPurge-1.0-jar-with-dependencies.jar ReadITunesU  site_id.txt


2. DownloadITunesuFile.java downloads itunesu content to local drive, based on given list of site ids. 

Before use, one need to change the program for required credentials and server information. 

java -cp target/iTunesuDataPurge-1.0-jar-with-dependencies.jar DownloadITunesuFile site_id.txt <download_working_path>


3. RemoveITunesUCourses.java remove the itunesu course from iTunesu server, based on a given list of site ids.  

java -cp target/iTunesuDataPurge-1.0-jar-with-dependencies.jar RemoveITunesUCourses site_id.txt
