'''

@author: Kyle Dove

Purpose: Pull data from Oracle database. Download exemption file from Google 
Drive. Remove exemptions from list. Properly format CSV file to be given to 
communications team.

'''

import logging
import time
import csv

#setup log

#create logger 'canvas_scraper'
logger = logging.getLogger('csv_formatter')
logger.setLevel(logging.INFO)
logdate = time.strftime("%Y%m%d")

#create file handler
fh = logging.FileHandler('csvFormatterLog_' + logdate + '.log')
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

logger.info('Script Initiated')

#Variables
exceptions = []
entries = []
moddedEntries = []

#Get the site exceptions that we are checking
with open('itunes_exceptions - Sheet 1.csv') as f:
    for line in f:
    	exceptions.append(line.rstrip('\n'))

for n, exception in enumerate(exceptions):
	splits = exception.split(',')
	siteId = splits[0]
	exceptions[n] = siteId

for exception in exceptions:
	logger.info('Exception Site Id: ' + str(exception))

#Get the sites that we are checking
with open('export.csv') as f:
    for line in f:
    	entries.append(line.rstrip('\n'))

#priming read
prevName = ''
row = ''

with open('outputReport.csv', 'wb') as csvfile:
	for entry in entries:
		newEntries = entry.split(',')
		name = newEntries[0].strip('\"')
		title = newEntries[1].strip('\"')
		siteId = newEntries[2].strip('\"')
		if siteId in exceptions:
			#skip
			continue
		if name != prevName:
			logger.info('Row: ' + row)
			csvfile.write(row + '\n')
			modName = name
			if '@' not in modName and entry != entries[0]:
				modName = modName + '@umich.edu'
			row = modName + ',' + title + ',' + siteId
		else:
			row = row + ',' + title + ',' + siteId
		prevName = name
	logger.info('Row: ' + row)
	csvfile.write(row + '\n')

logger.info('Length: ' + str(len(entries)))
logger.info('Script Completed - Done')