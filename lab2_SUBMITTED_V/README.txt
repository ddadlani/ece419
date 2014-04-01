ECE419 Lab 2 Submission

Geetika Saksena 998672191
Divya Dadlani 999181772

There are two .sh files:
run.sh runs the mazewar application. It takes in two arguments, serverhost and serverport.
mazeserver.sh runs the mazewar server. It takes in one argument, the port for the server.

The Mazewar application now has two dialog boxes for input: 
The first one takes in the player name.
The second one takes in the number of players. You need to enter the same number of players in each machine that you are using to run the application.

The number of players is static. You cannot add any more players after the game has started.
Similarly, the game won't start if fewer players connect than the number given.

The game can be run on separate machines only. When the game is run on the same machine, the key press connects with both the local clients on that machine.
