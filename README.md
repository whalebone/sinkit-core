# Testing


## MANAGED

## Expected env vars

see $PROJECT_HOME/.circleci/config.yml

## Run ISPN, Elastic
They should run on ports 9200, 9300 (Elastic) and 11322(ISPN)

## Initialize Elastic

curl -XPUT localhost:9200/_template/iocs -d @integration-tests/src/test/resources/elastic_iocs.json
curl -XPUT localhost:9200/_template/logs -d @integration-tests/src/test/resources/elastic_logs.json
curl -XPUT localhost:9200/_template/passivedns -d @integration-tests/src/test/resources/elastic_passivedns.json


## mvn integration-test -Parq-wildfly-managed -Dhotrod_host=127.0.0.1 -Dhotrod_port=11322



## Basic Config 


###Feeds configurations curl -i -H "Content-Type: application/json;charset=UTF-8"
-H "Accept: application/json;charset=UTF-8"
-H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}"
-X POST --data '[{"dns_client":"94.0.0.0/1","settings":{"some-intelmq-feed-to-sink":"S","some-feed-to-log":"L"},
"customer_id":666,"customer_name":"Some Name"}]' http://feedcore-lb:8080/sinkit/rest/rules/all

###Getting stats curl -i -H "Content-Type: application/json;charset=UTF-8"
-H "Accept: application/json;charset=UTF-8"
-H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}"
-X GET http://feedcore-lb:8080/sinkit/rest/stats

###Add an IoC - as FQDN curl -i -H "Content-Type: application/json;charset=UTF-8"
-H "Accept: application/json;charset=UTF-8"
-H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}"
-X POST --data '{"feed":{"name":"some-intelmq-feed-to-sink","url":"http://example.com/feed.txt"},
"classification":{"type": "phishing","taxonomy": "Fraud"},"raw":"aHwwwwfdfBmODQ2N244iNGZiNS8=",
"source":{"fqdn":"evil-domain-that-is-to-be-listed.cz","bgp_prefix":"some_prefix","asn":"3355556",
"asn_name":"any_name","geolocation":{"cc":"RU","city":"City","latitude":"85.12645","longitude":"-12.9788"}},"time":{"observation":"2015-12-12T22:52:58+02:00"},
"protocol":{"application":"ssh"},"description":{"text":"description"}}'
http://feedcore-lb:8080/sinkit/rest/blacklist/ioc/

###Add an IoC - as IP curl -i -H "Content-Type: application/json;charset=UTF-8"
-H "Accept: application/json;charset=UTF-8"
-H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}"
-X POST --data '{"feed":{"name":"some-intelmq-feed-to-sink","url":"http://example.com/feed.txt"},
"classification":{"type": "phishing","taxonomy": "Fraud"},"raw":"aHwwwwfdfBmODQ2N244iNGZiNS8=",
"source":{"ip":"93.184.216.34","bgp_prefix":"some_prefix","asn":"3355556",
"asn_name":"any_name","geolocation":{"cc":"RU","city":"City","latitude":"85.12645","longitude":"-12.9788"}},"time":{"observation":"2015-12-12T22:52:58+02:00"},
"protocol":{"application":"ssh"},"description":{"text":"description"}}'
http://feedcore-lb:8080/sinkit/rest/blacklist/ioc/

###Check whether IP or FQDN is listed curl -i -H "Content-Type: application/json;charset=UTF-8"
-H "Accept: application/json;charset=UTF-8"
-H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}"
-X GET http://feedcore-lb:8080/sinkit/rest/blacklist/dns//

###Add global whitelist IP entry curl -i -H "Content-Type: application/json;charset=UTF-8"
-H "Accept: application/json;charset=UTF-8"
-H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}"
-X POST --data '{"feed":{"name":"name_of_whitelist"},"source":{"ip":"83.215.22.31"}}"'
http://feedcore-lb:8080/sinkit/rest/whitelist/ioc/

###Add global whitelist FQDN entry curl -i -H "Content-Type: application/json;charset=UTF-8"
-H "Accept: application/json;charset=UTF-8"
-H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}"
-X POST --data '{"feed":{"name":"name_of_whitelist"},"source":{"fqdn":"trusted.net"}}"'
http://feedcore-lb:8080/sinkit/rest/whitelist/ioc/

###Get global whitelist entry curl -i -H "Content-Type: application/json;charset=UTF-8"
-H "Accept: application/json;charset=UTF-8"
-H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}"
-X GET http://feedcore-lb:8080/sinkit/rest/whitelist/record/

###Remove whitelist entry manually curl -i -H "Content-Type: application/json;charset=UTF-8"
-H "Accept: application/json;charset=UTF-8"
-H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}"
-X DELETE http://feedcore-lb:8080/sinkit/rest/whitelist/record/

###Get size of whitelist curl -i -H "Content-Type: application/json;charset=UTF-8"
-H "Accept: application/json;charset=UTF-8"
-H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}"
-X GET http://feedcore-lb:8080/sinkit/rest/whitelist/stats/


