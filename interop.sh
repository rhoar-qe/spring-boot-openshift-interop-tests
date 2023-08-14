#!/usr/bin/env bash

set -e

# Runner for interop testing
# expects unmodified git repo and will run a smoke tests from it

if [ $# -ne 4 ]
then
  echo "Expect 4 arguments: <Openshift URL> <Openshift project> <Openshift username> <Openshift password>" >&2
  echo "Example: https://api.ocp4.dynamic.rhoar:6443  springboot  rhoar  rhoar" >&2
  exit 1
fi

OPENSHIFT_URL=$1
OPENSHIFT_PROJECT=$2
OPENSHIFT_USERNAME=$3
OPENSHIFT_PASSWORD=$4

SPRING_BOOT_VERSION="2.7.12"

# ********* prepare test.properties file ***************

# if some test.properties file already exists, backup it
if [ -f test.properties ]
then
  mv test.properties test.properties.backup
fi

cat << EOF >> test.properties
xtf.openshift.url=$OPENSHIFT_URL
xtf.openshift.master.username=$OPENSHIFT_USERNAME
xtf.openshift.master.password=$OPENSHIFT_PASSWORD
xtf.config.master.namespace=$OPENSHIFT_PROJECT
xtf.bm.namespace=$OPENSHIFT_PROJECT
EOF


# ****** Set SB version in parent pom file *******
PARENT_POM="test-sb/src/test/resources/apps/springboot2/parent-msa-sb/pom.xml"
if cat $PARENT_POM | grep "SET_SB_VERSION" > /dev/null; then
  sed -i -e 's/<version.org.springframework.boot>SET_SB_VERSION<\/version.org.springframework.boot>/<version.org.springframework.boot>'$SPRING_BOOT_VERSION'<\/version.org.springframework.boot>/g' $PARENT_POM
fi

# ******** run the tests *****
mvn clean verify -Pinterop -PnoAdmin -Dversion.org.springframework.boot=$SPRING_BOOT_VERSION

#store junit report
cp test-sb/target/surefire-reports/*.xml junit-report.xml
