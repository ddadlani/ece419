#!/bin/bash
#$1 = hostname of MazeServer
#$2 = port of MazeServer

JAVA_HOME=/cad2/ece419s/java/jdk1.6.0/

${JAVA_HOME}/bin/java Mazewar ug161.eecg.utoronto.ca 3344

#${JAVA_HOME}/bin/java Mazewar $1 $2
