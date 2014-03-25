ECE419H1 S Distributed Systems Lab 3 (2014)

Team members: 
Geetika Saksena		998672191
Divya Dadlani		999181772

This distributed implementation of the Mazewar game supports dynamic client joining
and leaving.

The game assumes at most 10 clients, though it can handle more by changing the PID
implementation.

It is sequentially consistent throughout the game, except for after a client 
gets vaporized. It will then resume consistency after the vaporized client 
moves.

./server.sh runs the naming service

./run.sh <hostname of naming service> 6000 runs the mazewar
