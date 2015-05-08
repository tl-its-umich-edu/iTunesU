# iTunesU
Scripts for generating email notification related to iTunesU data purge

Installing cx_Oracle module on local Mac can be found at:
https://gist.github.com/thom-nic/6011715

cx_Oracle must be installed because it doesn't come pre-installed with Python.
Don't use pip!
Instead of using pip, following the directions located here:
http://joelvasallo.com/?p=276

When the instructions from the above link have been followed and completed, it is possible that you can update the cx_Oracle module with pip

'sudo pip install --upgrade cx_Oracle'

NOTE: If you are using Sublime or some other text editor to run the script, you may encounter the following error:

	Traceback (most recent call last):
	  File "/Users/<username>/Desktop/Python Oracle/OracleTesting.py", line 33, in <module>
	    import cx_Oracle
	ImportError: dlopen(/Library/Python/2.7/site-packages/cx_Oracle.so, 2): Library not loaded: /ade/b/2649109290/oracle/rdbms/lib/libclntsh.dylib.11.1
	  Referenced from: /Library/Python/2.7/site-packages/cx_Oracle.so
	  Reason: image not found
	[Finished in 0.3s with exit code 1]

I believe this is because... before you can run the script you need to load the bash_profile with the following command:
source ~/.bash_profile

I don't know of a way to do that in Sublime, but it may be possible with the following instructions (unverified):
https://blog.penjee.com/setting-up-python-on-mac/

However, the script can be executed in a terminal. So if you ever want to run the script in a terminal, here's how:

1. Open terminal

2. Run the following command:
'source ~/.bash-profile'

3. Run the python script where it was placed:
'python ~/<script location>/iTunesUPurgeListGenerator.py'

NOTE: I received the following error about a week after having the connection to the database working:
	cx_Oracle.DatabaseError: ORA-21561: OID generation failed
	The reason for this error has been shown to be related specifically to MACs. To remedy this issue, do the following:

	1. Open terminal.
	2. Type 'hostname' in terminal and run. This will return the hostname of your local machine. For example purposes, let's suppose the hostname returned is 'myMac'
	3. In the same terminal type 'sudo vi /etc/hosts' - this will open the hosts file for your local machine. You may need to enter your password.
	4. Your terminal will now be a VI editor. Press 'i' on the keyboard to turn on insert mode.
	5. You should see a line that reads: '127.0.0.1 localhost'. Edit this line by adding the hostname found in step 2 to the end of the line after a space: '127.0.0.1 localhost myMac'
	6. Press 'esc' on the keyboard to exit insert mode.
	7. Type ':wq' to save your file and exit VI. 

	After this modification you should find that the 'cx_Oracle.DatabaseError: ORA-21561: OID generation failed' error on the script goes away.

4. You may run into the script launching a Google Web page asking to allow authorization to access Google Drive. Clicking 'accept' in the browser will allow this to move forward.