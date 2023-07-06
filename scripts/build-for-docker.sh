#!/bin/bash

# This script prepares Docker Dockerfiles and Contexts for the UCS modules
# If the flag --build-images is specified, it also builds the Docker images

# Fail script with error if any command fails
#set -e

## Build the Jar files for the UCS modules, if needed
# the package phase will make a folder for each entity in the 'apps' folder
FILE=apps/UCSDht/UCSDht.jar
if [ -f "$FILE" ]; then
    echo "$FILE exists."
else
    echo "$FILE does not exist."
    echo "Building UCS modules..."
    ./scripts/build-ucs.sh
fi

## Create working directory for image building
mkdir -p docker-build/ucs
cd docker-build

# Copy needed files (jar files, library files, and entrypoint script)
cp -r ../apps/* ./ucs
cp ../scripts/run_UCSDht.sh ./ucs

## Create base Dockerfile. Initial part is same for all images.
#  Setting the timezone, otherwise the default is UTC.

echo 'FROM eclipse-temurin:8-jdk-jammy' > Dockerfile.base
echo 'WORKDIR /apps' >> Dockerfile.base
echo 'ENV TZ="Europe/Rome"' >> Dockerfile.base
echo 'RUN apt-get -y update && \' >> Dockerfile.base
echo '    apt-get install -yq tzdata && \' >> Dockerfile.base
echo '    ln -fs /usr/share/zoneinfo/Europe/Rome /etc/localtime && \' >> Dockerfile.base
echo '    dpkg-reconfigure -f noninteractive tzdata && \' >> Dockerfile.base
echo '    mkdir -p /apps/lib' >> Dockerfile.base
echo '' >> Dockerfile.base


## Prepare to build images
cd ucs

# Note that entrypoints must be adapted according to the location of entities and DHT.
# See the docker-compose.yml for a prepared setup.

# UCSDht

dockerfile=Dockerfile-UCSDht
cp ../Dockerfile.base $dockerfile
echo 'RUN mkdir -p /apps/UCSDht' >> $dockerfile
echo "ADD UCSDht/UCSDht.jar /apps/UCSDht" >> $dockerfile
echo 'ADD lib /apps/lib/' >> $dockerfile
echo 'ADD run_UCSDht.sh /' >> $dockerfile
echo 'RUN chmod +x /run_UCSDht.sh' >> $dockerfile
echo '' >> $dockerfile
echo 'ENTRYPOINT ["/run_UCSDht.sh"]' >> $dockerfile

#echo 'ENTRYPOINT ["java", "-jar", "UCSDht/UCSDht.jar"]' >> $dockerfile

if [ "$1" == "--build-images" ]
then
  docker build -f $dockerfile -t usage-control-engine .
fi


# PEPDht
dockerfile=Dockerfile-PEPDht
cp ../Dockerfile.base $dockerfile
echo 'RUN mkdir -p /apps/PEPDht' >> $dockerfile
echo "ADD PEPDht/PEPDht.jar /apps/PEPDht" >> $dockerfile
echo 'ADD lib /apps/lib/' >> $dockerfile
echo '' >> $dockerfile
echo 'ENTRYPOINT ["java", "-jar", "PEPDht/PEPDht.jar"]' >> $dockerfile

if [ "$1" == "--build-images" ]
then
  docker build -f $dockerfile -t pep-java .
fi


rm ../Dockerfile.base