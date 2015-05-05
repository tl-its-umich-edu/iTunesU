'''

@author: Kyle Dove

Purpose: Pull data from Oracle database. Download exemption file from Google 
Drive. Remove exemptions from list. Properly format CSV file to be given to 
communications team..

'''
#Imports
import logging
import time
import csv
import json
import cx_Oracle
from datetime import date, timedelta
from apiclient.discovery import build
from httplib2 import Http
from oauth2client import file, client, tools
from apiclient import errors
from apiclient import http

#Variables
exceptions = []
entries = []
moddedEntries = []
sqlDate = date.today() - timedelta(days=365)
sqlDate = sqlDate.strftime("%y")
queryString = """
  SELECT t5.eid, t2.title, t1.site_id
  FROM SAKAI_SITE_TOOL t1, sakai_site t2, SAKAI_REALM t3, SAKAI_REALM_RL_GR t4, SAKAI_USER_ID_MAP t5, SAKAI_REALM_ROLE t6
  where t1.REGISTRATION='sakai.iTunesU'
  and t1.SITE_ID = t2.SITE_ID
  and t2.createdon < to_date('05/01/""" + sqlDate + """ 00:00:00','mm/dd/yy hh24:mi:ss') 
  and '/site/' || t1.site_id = t3.realm_id
  and t3.realm_key=t4.realm_key
  and (t6.role_name='Instructor' or t6.role_name='Owner')
  and t4.role_key = t6.role_key
  and t4.user_id = t5.user_id
  order by t5.eid, t1.site_id
"""

#setup log

#create logger 'iTunesUPurgeListGenerator'
logger = logging.getLogger('iTunesUPurgeListGenerator')
logger.setLevel(logging.INFO)
logdate = time.strftime("%Y%m%d")

#create file handler
fh = logging.FileHandler('iTunesUPurgeListGenerator_' + logdate + '.log')
fh.setLevel(logging.INFO)

#create console handler
ch = logging.StreamHandler()
ch.setLevel(logging.INFO)

#create formatter and add it to the handlers
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
fh.setFormatter(formatter)
ch.setFormatter(formatter)

#add the handlers to the logger
logger.addHandler(fh)
logger.addHandler(ch)

def download_file(service, drive_file):
    download_url = drive_file['exportLinks']['text/csv']
    logger.info('DownloadUrl: ' + download_url)
    if download_url:
            resp, content = service._http.request(download_url)
            if resp.status == 200:
                    logger.info('Status: %s' % resp)
                    title = drive_file.get('title')
                    path = './'+title+'.csv'
                    file = open(path, 'wb')
                    file.write(content)
            else:
                    logger.info('An error occurred: %s' % resp)
                    return None
    else:
            # The file doesn't have any content stored on Drive.
            return None

logger.info('Script Initiated')

logger.info('SQL Date: ' + sqlDate)

#Open properties file and set properties
with open('propertiesProd.json') as data_file:    
    data = json.load(data_file)

logger.debug('url: ' + str(data['URL']))
logger.debug('port: ' + str(data['PORT']))
logger.debug('sid: ' + str(data['SID']))
logger.debug('userid: ' + str(data['USERID']))
logger.debug('password: ' + str(data['PASSWORD']))
logger.debug('driveFile: ' + str(data['DRIVE_FILE']))

#Connect to database
dsnStr = cx_Oracle.makedsn(data['URL'], data['PORT'], data['SID'])
con = cx_Oracle.connect(user=data['USERID'], password=data['PASSWORD'], dsn=dsnStr)
cursor = con.cursor()

logger.info('Version: ' + str(con.version)) #Shows connection was successful

logger.debug('Query String: ' + queryString)

#Run query and save output to file
cursor.execute(queryString)
with open('output_file.csv', 'wb') as fout:
    writer = csv.writer(fout)
    writer.writerow([ i[0] for i in cursor.description ]) # heading row
    writer.writerows(cursor.fetchall())
con.close() #Close connection

#Download exceptions file
CLIENT_SECRET = 'tl_client_secret.json'
SCOPES = [
	'https://www.googleapis.com/auth/drive.readonly',
	'https://www.googleapis.com/auth/drive',
	'https://www.googleapis.com/auth/drive.appdata',
	'https://www.googleapis.com/auth/drive.apps.readonly',
	'https://www.googleapis.com/auth/drive.file',
	'https://www.googleapis.com/auth/drive.readonly'
]
store = file.Storage('storage.json')
creds = store.get()
if not creds or creds.invalid:
    flow = client.flow_from_clientsecrets(CLIENT_SECRET, ' '.join(SCOPES))
    creds = tools.run(flow, store)
DRIVE = build('drive', 'v2', http=creds.authorize(Http()))
file_Id = str(data['DRIVE_FILE'])
gFile = DRIVE.files().get(fileId = file_Id).execute()
download_file(DRIVE, gFile)

#Get the site exceptions that we are checking
with open('itunes_exceptions.csv') as f:
    for line in f:
    	exceptions.append(line.rstrip('\n\r '))
for n, exception in enumerate(exceptions):
	splits = exception.split(',')
	siteId = splits[0]
	exceptions[n] = siteId
for exception in exceptions:
	logger.debug('Exception Site Id: ' + str(exception))

#Get the sites that we are checking
with open('output_file.csv', 'rb') as f:
    for line in f:
    	entries.append(line.rstrip('\n\r '))

prevName = ''
row = ''

with open('iTunesUPurgeEmailList.csv', 'wb') as csvfile:
	for entry in entries:
		logger.debug('Entry: ' + entry)
		fields = entry.split(',')
		name = fields[0]
		title = fields[1]
		siteId = fields[2]
		if siteId in exceptions:
			#skip
			continue
		if name != prevName:
			logger.debug('Row: ' + row)
			if row != '':
				csvfile.write(row + '\n')
			modName = name
			#if uMich account, input only has uniqName so add '@umich.edu'. Otherwise, @someplace.com already on name.
			if '@' not in modName and entry != entries[0]:
				modName = modName + '@umich.edu'
			row = modName + ',' + title + ',' + siteId
			row = row.rstrip('\n')
		else:
			row = row + ',' + title + ',' + siteId
			row = row.rstrip('\n')
		prevName = name

	logger.debug('Row: ' + row)
	csvfile.write(row + '\n')

logger.info('Length: ' + str(len(entries)))
logger.info('Script Completed - Done')