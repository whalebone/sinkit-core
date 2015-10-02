#!/bin/bash

# @author Michal Karm Babacek

# Debug logging
echo "STAT: `networkctl status`" >> /opt/sinkit/ip.log
echo "STAT ${SINKIT_NIC:-eth0}: `networkctl status ${SINKIT_NIC:-eth0}`" >> /opt/sinkit/ip.log

# Wait for the interface to wake up
TIMEOUT=20
MYIP=""
while [[ "${MYIP}X" == "X" ]] && [[ "${TIMEOUT}" -gt 0 ]]; do
    echo "Loop ${TIMEOUT}" >> /opt/sinkit/ip.log
    MYIP="`networkctl status ${SINKIT_NIC:-eth0} | awk '{if($1~/Address:/){printf($2);}}'`"
    export MYIP
    let TIMEOUT=$TIMEOUT-1
    if [[ "${MYIP}X" != "X" ]]; then break; else sleep 1; fi
done
echo -e "MYIP: ${MYIP}\nMYNIC: ${SINKIT_NIC:-eth0}" >> /opt/sinkit/ip.log
if [[ "${MYIP}X" == "X" ]]; then 
    echo "${SINKIT_NIC:-eth0} Interface error. " >> /opt/sinkit/ip.log
    exit 1
fi

# Replace NIC
sed -i "s/@SINKITNIC@/${SINKIT_NIC:-eth0}/g" ${WF_CONFIG}

sed -i "s/<core-environment>/<core-environment node-identifier=\"${TUTUM_CONTAINER_FQDN}\">/g" ${WF_CONFIG}

/opt/sinkit/wildfly/bin/standalone.sh \
 -c standalone.xml \
 -Djava.net.preferIPv4Stack=true \
 -Djboss.modules.system.pkgs=org.jboss.byteman \
 -Djava.awt.headless=true \
 -Djgroups.bind_addr=${MYIP} \
 -Djgroups.tcp.address=${MYIP} \
 -Djboss.bind.address.management=${MYIP} \
 -Djboss.bind.address=${MYIP} \
 -Djboss.bind.address.unsecure=${MYIP} \
 -Djgroups.udp.mcast_addr=228.6.7.8 \
 -Djgroups.udp.mcast_port=46655 \
 -Djboss.node.name="${TUTUM_CONTAINER_FQDN}"
