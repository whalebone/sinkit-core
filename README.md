# sinkit-core
Integration tests: [![Circle CI](https://circleci.com/gh/intfeed/sinkit-core.svg?style=svg)](https://circleci.com/gh/intfeed/sinkit-core)
A prototype of a toy project featuring Infinispan dist cache.

## Expected env vars
    export SINKIT_ACCESS_TOKEN=
    export SINKIT_SINKHOLE_IP=
    export SINKIT_ELASTIC_HOST=
    export SINKIT_ELASTIC_PORT=
    export SINKIT_POSTGRESQL_DB_HOST=
    export SINKIT_POSTGRESQL_DB_PORT=
    export SINKIT_POSTGRESQL_DB_NAME=
    export SINKIT_POSTGRESQL_PASS=
    export SINKIT_POSTGRESQL_USER=
    export SINKIT_VIRUS_TOTAL_SKIP=
    export SINKIT_VIRUS_TOTAL_API_KEY=
    export SINKIT_SINKHOLE_IPV6=
    export SINKIT_IOC_DEACTIVATOR_SKIP=
    export SINKIT_IOC_ACTIVE_HOURS=

## Basic config
###Feeds configurations
    curl -i -H "Content-Type: application/json;charset=UTF-8" \
    -H "Accept: application/json;charset=UTF-8" \
    -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" \
    -X POST --data '[{"dns_client":"94.0.0.0/1","settings":{"some-intelmq-feed-to-sink":"S","some-feed-to-log":"L"}, \
    "customer_id":666,"customer_name":"Some Name"}]' http://feedcore-lb:8080/sinkit/rest/rules/all

###Getting stats
    curl -i -H "Content-Type: application/json;charset=UTF-8" \
    -H "Accept: application/json;charset=UTF-8" \
    -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" \
    -X GET http://feedcore-lb:8080/sinkit/rest/stats

###Add an IoC - as FQDN
    curl -i -H "Content-Type: application/json;charset=UTF-8" \
    -H "Accept: application/json;charset=UTF-8" \
    -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" \
    -X POST --data '{"feed":{"name":"some-intelmq-feed-to-sink","url":"http://example.com/feed.txt"}, \
    "classification":{"type": "phishing","taxonomy": "Fraud"},"raw":"aHwwwwfdfBmODQ2N244iNGZiNS8=", \
    "source":{"fqdn":"evil-domain-that-is-to-be-listed.cz","bgp_prefix":"some_prefix","asn":"3355556", \
    "asn_name":"any_name","geolocation":{"cc":"RU","city":"City","latitude":"85.12645","longitude":"-12.9788"}},"time":{"observation":"2015-12-12T22:52:58+02:00"}, \
    "protocol":{"application":"ssh"},"description":{"text":"description"}}' \
    http://feedcore-lb:8080/sinkit/rest/blacklist/ioc/

###Add an IoC - as IP
    curl -i -H "Content-Type: application/json;charset=UTF-8" \
    -H "Accept: application/json;charset=UTF-8" \
    -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" \
    -X POST --data '{"feed":{"name":"some-intelmq-feed-to-sink","url":"http://example.com/feed.txt"}, \
    "classification":{"type": "phishing","taxonomy": "Fraud"},"raw":"aHwwwwfdfBmODQ2N244iNGZiNS8=", \
    "source":{"ip":"93.184.216.34","bgp_prefix":"some_prefix","asn":"3355556", \
    "asn_name":"any_name","geolocation":{"cc":"RU","city":"City","latitude":"85.12645","longitude":"-12.9788"}},"time":{"observation":"2015-12-12T22:52:58+02:00"}, \
    "protocol":{"application":"ssh"},"description":{"text":"description"}}' \
    http://feedcore-lb:8080/sinkit/rest/blacklist/ioc/

###Check whether IP or FQDN is listed
    curl -i -H "Content-Type: application/json;charset=UTF-8" \
    -H "Accept: application/json;charset=UTF-8" \
    -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" \
    -X GET http://feedcore-lb:8080/sinkit/rest/blacklist/dns/<DNS client IP>/<domain or IP to check>