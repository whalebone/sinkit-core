# Testing

    mvn clean install -Parq-wildfly-remote -Dsinkit_management_address=192.168.122.156 -Dsinkit_management_port=9990 -Dsinkit_username=karm -Dsinkit_password="I almost left it here" -Dhotrod_host=192.168.122.156 -Dhotrod_port=11322

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
    export SINKIT_GSB_API_KEY=
    export SINKIT_GSB_FULLHASH_URL=
    export SINKIT_WHITELIST_VALID_HOURS=
    export SINKIT_LOGSTASH_URL=

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

###Add global whitelist IP entry
    curl -i -H "Content-Type: application/json;charset=UTF-8" \
    -H "Accept: application/json;charset=UTF-8" \
    -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" \
    -X POST --data '{"feed":{"name":"name_of_whitelist"},"source":{"ip":"83.215.22.31"}}"' \
    http://feedcore-lb:8080/sinkit/rest/whitelist/ioc/

###Add global whitelist FQDN entry
    curl -i -H "Content-Type: application/json;charset=UTF-8" \
    -H "Accept: application/json;charset=UTF-8" \
    -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" \
    -X POST --data '{"feed":{"name":"name_of_whitelist"},"source":{"fqdn":"trusted.net"}}"' \
    http://feedcore-lb:8080/sinkit/rest/whitelist/ioc/

###Get global whitelist entry
    curl -i -H "Content-Type: application/json;charset=UTF-8" \
    -H "Accept: application/json;charset=UTF-8" \
    -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" \
    -X GET http://feedcore-lb:8080/sinkit/rest/whitelist/record/<domain or IP to check>

###Remove whitelist entry manually
    curl -i -H "Content-Type: application/json;charset=UTF-8" \
    -H "Accept: application/json;charset=UTF-8" \
    -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" \
    -X DELETE http://feedcore-lb:8080/sinkit/rest/whitelist/record/<domain or IP to check>

###Get size of whitelist
    curl -i -H "Content-Type: application/json;charset=UTF-8" \
    -H "Accept: application/json;charset=UTF-8" \
    -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" \
    -X GET http://feedcore-lb:8080/sinkit/rest/whitelist/stats/

## Google Safe Browsing API
###Add a hash prefix
    curl -i -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" -X PUT http://localhost:8080/sinkit/rest/gsb/cf4b367e

###Remove a hash prefix
    curl -i -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" -X DELETE http://localhost:8080/sinkit/rest/gsb/cf4b367e

###Get number of cached prefixes
    curl -i -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" -X GET http://localhost:8080/sinkit/rest/gsb/stats

###Cleare GSB cache (this can be requested by Google itself)
    curl -i -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" -X DELETE http://localhost:8080/sinkit/rest/gsb/

###Lookup for URL (for testing purposes)
    curl -i -H "X-sinkit-token: ${SINKIT_ACCESS_TOKEN}" -X GET http://localhost:8080/sinkit/rest/gsb/lookup/google.com

