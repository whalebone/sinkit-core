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
    if [[ "${MYIP}" == ${SINKIT_ADDR_PREFIX:-10}* ]]; then
        break;
    else 
        MYIP=""
        sleep 1;
    fi
done
echo -e "MYIP: ${MYIP}\nMYNIC: ${SINKIT_NIC:-eth0}" >> /opt/sinkit/ip.log
if [[ "${MYIP}X" == "X" ]]; then 
    echo "${SINKIT_NIC:-eth0} Interface error. " >> /opt/sinkit/ip.log
    exit 1
fi

# Replace NIC
sed -i "s/@SINKITNIC@/${SINKIT_NIC:-eth0}/g" ${WF_CONFIG}

# Replace Logging level
sed -i "s/@SINKITLOGGING@/${SINKIT_LOGLEVEL:-INFO}/g" ${WF_CONFIG}

#CONTAINER_NAME=`echo ${DOCKERCLOUD_CONTAINER_FQDN}|sed 's/\([^\.]*\.[^\.]*\).*/\1/g'`

if [ "`echo \"${HOSTNAME}\" | wc -c`" -gt 24 ]; then
    echo "ERROR: HOSTNAME ${HOSTNAME} must be up to 24 characters long."
    exit 1
fi

sed -i "s/<core-environment>/<core-environment node-identifier=\"${HOSTNAME}\">/g" ${WF_CONFIG}

# Generate and configure JKS from injected certificates
if [[ "${SINKIT_CA_CRT_BASE64}X" == "X" ]]; then
    echo "SINKIT_CA_CRT_BASE64 must contain PEM certificate, base64 encoded but was empty."
    exit 1
fi
if [[ "${SINKIT_SERVER_CRT_BASE64}X" == "X" ]]; then
    echo "SINKIT_SERVER_CRT_BASE64 must contain PEM certificate, base64 encoded but was empty."
    exit 1
fi
if [[ "${SINKIT_SERVER_KEY_BASE64}X" == "X" ]]; then
    echo "SINKIT_SERVER_KEY_BASE64 must contain PEM certificate, base64 encoded but was empty."
    exit 1
fi
if [[ "${SINKIT_KEYSTORE_PASS}X" == "X" ]]; then
    echo "SINKIT_KEYSTORE_PASS must contain a string but was empty."
    exit 1
fi

echo ${SINKIT_CA_CRT_BASE64} | base64 -d  > /opt/sinkit/certs/oraculum_ca.crt
echo ${SINKIT_SERVER_CRT_BASE64} | base64 -d > /opt/sinkit/certs/oraculum_server.crt
echo ${SINKIT_SERVER_KEY_BASE64} | base64 -d > /opt/sinkit/certs/oraculum_server.key

if ! [[ -s /opt/sinkit/certs/oraculum_ca.crt ]]; then
    echo "File /opt/sinkit/certs/oraculum_ca.crt must not be empty."
    exit 1
fi

if ! [[ -s /opt/sinkit/certs/oraculum_server.crt ]]; then
    echo "File /opt/sinkit/certs/oraculum_server.crt must not be empty."
    exit 1
fi

if ! [[ -s /opt/sinkit/certs/oraculum_server.key ]]; then
    echo "File /opt/sinkit/certs/oraculum_server.key must not be empty."
    exit 1
fi

yes | keytool -import -file /opt/sinkit/certs/oraculum_ca.crt -keystore /opt/sinkit/certs/ca-cert.jks -storepass "${SINKIT_KEYSTORE_PASS}"
openssl pkcs12 -export -in /opt/sinkit/certs/oraculum_server.crt -inkey /opt/sinkit/certs/oraculum_server.key -out /opt/sinkit/certs/oraculum_server.pfx -passout pass:"${SINKIT_KEYSTORE_PASS}"
echo -e "${SINKIT_KEYSTORE_PASS}\n${SINKIT_KEYSTORE_PASS}\n${SINKIT_KEYSTORE_PASS}" | keytool -importkeystore -destkeystore /opt/sinkit/certs/server-cert-key.jks -srckeystore /opt/sinkit/certs/oraculum_server.pfx -srcstoretype PKCS12

if ! [[ -s /opt/sinkit/certs/ca-cert.jks ]]; then
    echo "File /opt/sinkit/certs/ca-cert.jks must not be empty."
    exit 1
fi

if ! [[ -s /opt/sinkit/certs/server-cert-key.jks  ]]; then
    echo "File /opt/sinkit/certs/server-cert-key.jks  must not be empty."
    exit 1
fi

sed -i "s/@SSL_PROTOCOL@/${SINKIT_SSL_PROTOCOL:-TLSv1.2}/g" ${WF_CONFIG}
#sed -i "s/@ENABLED_CIPHER_SUITES@/${SINKIT_ENABLED_CIPHER_SUITES:-}/g" ${WF_CONFIG}
sed -i "s/@ORACULUM_SERVER_KEYSTORE_PATH@/\/opt\/sinkit\/certs\/server-cert-key.jks/g" ${WF_CONFIG}
sed -i "s/@ORACULUM_SERVER_KEYSTORE_PASS@/${SINKIT_KEYSTORE_PASS}/g" ${WF_CONFIG}
sed -i "s/@ORACULUM_SERVER_KEYSTORE_ALIAS@/1/g" ${WF_CONFIG}
sed -i "s/@ORACULUM_SERVER_VERIFY_CLIENT@/${SINKIT_VERIFY_CLIENT:-REQUIRED}/g" ${WF_CONFIG}
sed -i "s/@ORACULUM_TRUST_STORE_PATH@/\/opt\/sinkit\/certs\/ca-cert.jks/g" ${WF_CONFIG}
sed -i "s/@ORACULUM_TRUST_STORE_PASS@/${SINKIT_KEYSTORE_PASS}/g" ${WF_CONFIG}

# Management console access
if [[ "${SINKIT_MGMT_USER}X" != "X" ]] && [[ "${SINKIT_MGMT_PASS}X" != "X" ]]; then
    /opt/sinkit/wildfly/bin/add-user.sh -m -u "${SINKIT_MGMT_USER}" -p "${SINKIT_MGMT_PASS}"
fi

# NFS
/opt/sinkit/mount.sh

/opt/sinkit/wildfly/bin/standalone.sh \
 -b ${MYIP} \
 -c standalone.xml \
 -Djava.net.preferIPv4Stack=true \
 -Djboss.modules.system.pkgs=org.jboss.byteman \
 -Djava.awt.headless=true \
 -Djboss.bind.address.management=${MYIP} \
 -Djboss.bind.address=${MYIP} \
 -Djboss.bind.address.private=${MYIP} \
 -Djboss.node.name="${HOSTNAME}" \
 -Djboss.host.name="${HOSTNAME}" \
 -Djboss.qualified.host.name="${HOSTNAME}" \
 -Djboss.as.management.blocking.timeout=1800
