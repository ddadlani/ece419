README Lab 4
------------

Team members:
Divya Dadlani 	999181772
Geetika Saksena 998672191

Scripts:
clientdriver.sh <hostname>:<port>
jobtracker.sh <hostname>:<port>
worker.sh <hostname>:<port>
fileserver.sh <hostname>:<port> <filename>

The paths of zookeeper in the scripts and makefile can be redefined accordingly.

Notes:
. This system is fully functional as it handles failures and allows for scalability. 
. Every time you require to re-run the system after killing the jobtracker, 
  kindly delete all children of /jobs folder through zkCli.sh (as they are persistent)
. At all times, at least one Worker should be running.


		
