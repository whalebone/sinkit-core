#!/bin/bash
if [ $# -ne 2 ]
  then
    echo "You should run the script as: $0 <iocfile> <api_url>"
    exit 1
fi
TIMESTAMPNOW=`date -u +"%Y-%m-%dT%H:%M:%S%z"`
IFS=''
while read ioc; do
  curl -s -i -H "Content-Type: application/json;charset=UTF-8" -H "Accept: application/json;charset=UTF-8" -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" -X POST --data `echo $ioc | sed "s|\"observation\"\s*:\s*\"[^\"]*\"|\"observation\":\"$TIMESTAMPNOW\"|g"` $2 > /dev/null
done < $1
