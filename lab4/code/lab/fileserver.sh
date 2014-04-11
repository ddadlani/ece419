ZOOKEEPER=/homes/d/dadlanid/ece419/lab4/zookeeper

java -classpath ${ZOOKEEPER}/zookeeper-3.3.2.jar:${ZOOKEEPER}/lib/log4j-1.2.15.jar:. FileServer $1 $2
