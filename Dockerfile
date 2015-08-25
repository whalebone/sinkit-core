FROM fedora:22
MAINTAINER Michal Karm Babacek <karm@redhat.com>
LABEL description="Codename Feed: Sinkit Core POC"

ENV DEPS            java-1.8.0-openjdk-devel.x86_64 unzip wget gawk
ENV WILDFLY_VERSION 9.0.1.Final
ENV JBOSS_HOME      "/opt/sinkit/wildfly-${WILDFLY_VERSION}"
ENV JAVA_HOME       "/usr/lib/jvm/java-1.8.0"

RUN dnf -y update && dnf -y install ${DEPS} && dnf clean all
RUN useradd -s /sbin/nologin sinkit
RUN mkdir -p /opt/sinkit && chown sinkit /opt/sinkit && chgrp sinkit /opt/sinkit && chmod ug+rwxs /opt/sinkit

WORKDIR /opt/sinkit
USER sinkit

# ADD would run every rebuild
RUN wget http://download.jboss.org/wildfly/${WILDFLY_VERSION}/wildfly-${WILDFLY_VERSION}.zip && \
    unzip wildfly-${WILDFLY_VERSION}.zip && rm -rf wildfly-${WILDFLY_VERSION}.zip

# Circle CI builds and tests the archive, so no need for additional checks here.
ADD ear/target/sinkit-ear.ear /opt/sinkit/wildfly-${WILDFLY_VERSION}/standalone/deployments/

# Workaround for https://github.com/docker/docker/issues/5509
RUN ln -s /opt/sinkit/wildfly-${WILDFLY_VERSION} /opt/sinkit/wildfly

# TODO: Fix JGroups, remove garbage...
EXPOSE 8080/tcp
EXPOSE 46655/udp
EXPOSE 7500/udp
EXPOSE 55200/udp
EXPOSE 54200/udp
EXPOSE 45688/udp
EXPOSE 7800/udp

ENV WF_CONFIG /opt/sinkit/wildfly/standalone/configuration/standalone.xml

# Set NIC, this makes the ugly 0.0.0.0 work.
# Yeees, editing an XML file with AWK :-)
RUN awk '{ if ( $0 ~ /<inet-address value=/ ) { printf( "%s\n%s\n", $0, "        <nic name=\"${sinkit.nic:eth0}\"/>"); } else {print $0; } }' \
   ${WF_CONFIG} > ${WF_CONFIG}.tmp && mv ${WF_CONFIG}.tmp ${WF_CONFIG}

RUN echo 'JAVA_OPTS="\
 -server \
 -XX:+UseCompressedOops \
 -Xms${SINKIT_MS_RAM:-6144m} \
 -Xmx${SINKIT_MX_RAM:-6144m} \
 -XX:+HeapDumpOnOutOfMemoryError \
 -XX:HeapDumpPath=/opt/sinkit \
 -XX:+UseConcMarkSweepGC \
 -Djava.net.preferIPv4Stack=true \
 -Djboss.modules.system.pkgs=org.jboss.byteman \
 -Djava.awt.headless=true \
 -Dsinkit.nic=${SINKIT_NIC:-eth0} \
 -Djgroups.bind_addr=0.0.0.0 \
 -Djgroups.udp.mcast_addr=228.6.7.8 \
 -Djgroups.udp.mcast_port=46655 \
"' >> /opt/sinkit/wildfly/bin/standalone.conf

CMD ["/opt/sinkit/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-c", "standalone.xml"]