#!/bin/bash

SUNSPOT_HOME=/usr/local/SunSPOT/sdk-blue-080827
SOURCE_JAR=jzlib-source.jar
TARGET_JAR=jzlib.jar

mkdir tmp1 tmp2 tmp3

cd tmp1
jar -xf ../$SOURCE_JAR 
cd ..

javac -d tmp2 -target 1.2 -source 1.3 `find tmp1 -name "*.java"`
$SUNSPOT_HOME/bin/preverify -d tmp3 -classpath $SUNSPOT_HOME/lib/squawk_device.jar:tmp3 tmp2

[ -r $TARGET_JAR ] && rm $TARGET_JAR
jar -cf $TARGET_JAR -C tmp3 com

rm -rf tmp1 tmp2 tmp3

