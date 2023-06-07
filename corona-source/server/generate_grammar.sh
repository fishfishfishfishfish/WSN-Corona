#!/bin/bash

cd grammar

CP="`ls *.jar | tr '\n' ':'`." 
JARFILE=corona-querylanguage.jar
[ -r build ] && rm -rf build

# compiles the ANTLR grammar into Java source code
java -cp "$CP" org.antlr.Tool -o build CoronaQL.g
[ $? != 0 ] && cd .. && exit 1

# compiles the generated Java source code
javac -cp "$CP:build" -d build build/*.java

# jar's the generated Java class files and relocates it to /lib
cd build
[ -r $JARFILE ] && rm -f $JARFILE
jar -cf $JARFILE .
mv $JARFILE ../../lib/
cd ..

# back to root
cd ..

