#!/bin/sh
JAVA_HOME=/usr '/Applications/NetBeans/NetBeans 7.4.app/Contents/Resources/NetBeans/java/maven/bin/mvn' install:install-file -Dfile=/project2/jitsi/ice4j-read-only/ice4j.jar -DgroupId=org \
    -DartifactId=ice4j -Dversion=5.09 -Dpackaging=jar \
-DlocalRepositoryPath=repo

