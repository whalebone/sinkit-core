#!/bin/bash

# @author Michal Karm Babacek

# Hardcoded wait for the interface to wake up
sleep 2

# Debug logging
echo "STAT: `networkctl status`" >> /opt/sinkit/ip.log
echo "STAT ${SINKIT_NIC:-eth0}: `networkctl status ${SINKIT_NIC:-eth0}`" >> /opt/sinkit/ip.log
export MYIP="`networkctl status ${SINKIT_NIC:-eth0} | awk '{if($1~/Address:/){printf($2);}}'`"
echo -e "MYIP: ${MYIP}\nMYNIC: ${SINKIT_NIC:-eth0}" >> /opt/sinkit/ip.log

# Replace NIC
sed -i "s/@SINKITNIC@/${SINKIT_NIC:-eth0}/g" ${WF_CONFIG}

/opt/sinkit/wildfly/bin/standalone.sh \
 -c standalone.xml \
 -Djava.net.preferIPv4Stack=true \
 -Djboss.modules.system.pkgs=org.jboss.byteman \
 -Djava.awt.headless=true \
 -Djgroups.bind_addr=${MYIP} \
 -Djboss.bind.address.management=${MYIP} \
 -Djboss.bind.address=${MYIP} \
 -Djboss.bind.address.unsecure=${MYIP} \
 -Djgroups.udp.mcast_addr=228.6.7.8 \
 -Djgroups.udp.mcast_port=46655
