# Testing

## MANAGED


## Expected env vars

export SINKIT_ACCESS_TOKEN=user
export SINKIT_ADDR_PREFIX=127
export SINKIT_DNS_REQUEST_LOGGING_ENABLED=True
export SINKIT_ELASTIC_CLUSTER=archive
export SINKIT_ELASTIC_HOST=127.0.0.1
export SINKIT_ELASTIC_PORT=9300
export SINKIT_ELASTIC_REST_PORT=9200
export SINKIT_HOTROD_HOST=127.0.0.1
export SINKIT_HOTROD_PORT=11322
export SINKIT_HOTROD_CONN_TIMEOUT_S=300
export SINKIT_IOC_ACTIVE_HOURS=1
export IOC_ACTIVE_HOURS_ENV=1
export WHITELIST_VALID_HOURS_ENV=1
export SINKIT_IOC_DEACTIVATOR_SKIP=True
export SINKIT_LOCAL_CACHE_LIFESPAN=1000
export SINKIT_LOCAL_CACHE_SIZE=10
export SINKIT_LOGLEVEL=ALL
export SINKIT_MS_RAM=256m
export SINKIT_MX_RAM=256m
export SINKIT_NIC=lo
export SINKIT_SINKHOLE_IP=127.0.0.1
export SINKIT_SINKHOLE_IPV6=::1
export SINKIT_VERIFY_CLIENT=REQUIRED
export SINKIT_VIRUS_TOTAL_SKIP=True
export SINKIT_WHITELIST_VALID_HOURS=1
export SINKIT_MGMT_USER=user
export SINKIT_MGMT_PASS=user
export SINKIT_WHITELIST_PROTOSTREAM_GENERATOR_D_H_M_S="* * * */30"
export SINKIT_CUSTOMLIST_PROTOSTREAM_GENERATOR_D_H_M_S="* * * */30"
export SINKIT_IOC_PROTOSTREAM_GENERATOR_D_H_M_S="* * * */30"
export SINKIT_ALL_IOC_PROTOSTREAM_GENERATOR_D_H_M_S="* * * */30"
export SINKIT_ALL_CUSTOM_PROTOSTREAM_GENERATOR_D_H_M_S="* * * */30"
export SINKIT_GENERATED_PROTOFILES_DIRECTORY=/tmp/proto

## Run ISPN, Elastic
They should run on ports 9200, 9300 (Elastic) and 11322(ISPN)

## Initialize Elastic

export DATE=`date +%Y-%m-%d`

curl -XDELETE localhost:9200/iocs
curl -XDELETE localhost:9200/logs-$DATE

curl -XPUT localhost:9200/_template/iocs -d @integration-tests/src/test/resources/elastic_iocs.json
curl -XPUT localhost:9200/_template/logs -d @integration-tests/src/test/resources/elastic_logs.json
curl -XPUT localhost:9200/_template/passivedns -d @integration-tests/src/test/resources/elastic_passivedns.json

curl -XPUT localhost:9200/iocs
curl -XPUT localhost:9200/logs-$DATE

## mvn integration-test -Parq-wildfly-managed -Dhotrod_host=127.0.0.1 -Dhotrod_port=11322


