#!/bin/bash

#compilazione
javac -cp .;lib\jackson-annotations-2.9.7.jar;lib\jackson-core-2.9.7.jar;lib\jackson-databind-2.9.7.jar MainServer.java MainClient.java client/*.java risorse/*.java RMI/*.java utils/*.java server/*.java -d jar/
java -cp .;lib\jackson-annotations-2.9.7.jar;lib\jackson-core-2.9.7.jar;lib\jackson-databind-2.9.7.jar MainServer


