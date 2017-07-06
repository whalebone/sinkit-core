FROM fedora:25
MAINTAINER Michal Karm Babacek <karm@email.cz
LABEL description="Codename Feed: Sinkit Core POC"

ENV DEPS            java-1.8.0-openjdk-devel.x86_64 unzip wget gawk sed openssl jna.x86_64 jsch-agent-proxy-usocket-jna.noarch
ENV JBOSS_HOME      "/opt/sinkit/wildfly"
ENV JAVA_HOME       "/usr/lib/jvm/java-1.8.0"

RUN dnf -y update && dnf -y install ${DEPS} && dnf clean all
RUN useradd -s /sbin/nologin sinkit
RUN mkdir -p /opt/sinkit && chown sinkit /opt/sinkit && chgrp sinkit /opt/sinkit && chmod ug+rwxs /opt/sinkit

WORKDIR /opt/sinkit

EXPOSE 8080/tcp
EXPOSE 8443/tcp

ENV WILDFLY_VERSION 10.1.0.Final
ENV WF_CONFIG /opt/sinkit/wildfly/standalone/configuration/standalone.xml
RUN \
# Fetch Wildfly and Infinispan Server
    wget http://download.jboss.org/wildfly/${WILDFLY_VERSION}/wildfly-${WILDFLY_VERSION}.zip && \
    unzip wildfly-${WILDFLY_VERSION}.zip && rm -rf wildfly-${WILDFLY_VERSION}.zip && \
# Workaround for https://github.com/docker/docker/issues/5509
    ln -s /opt/sinkit/wildfly-${WILDFLY_VERSION} /opt/sinkit/wildfly

ADD standalone.xml ${WF_CONFIG}

RUN echo 'JAVA_OPTS="\
 -server \
 -Xms${SINKIT_MS_RAM:-1g} \
 -Xmx${SINKIT_MX_RAM:-1g} \
 -XX:MetaspaceSize=64M \
 -XX:MaxMetaspaceSize=512m \
 -XX:+UseG1GC \
 -XX:MaxGCPauseMillis=100 \
 -XX:InitiatingHeapOccupancyPercent=70 \
 -XX:+HeapDumpOnOutOfMemoryError \
 -XX:HeapDumpPath=/opt/sinkit \
"' >> /opt/sinkit/wildfly/bin/standalone.conf
RUN mkdir -p /opt/sinkit/wildfly/standalone/log/ && mkdir -p /opt/sinkit/certs && mkdir -p /opt/sinkit/protobuf && \
    chown sinkit /opt/sinkit/ -R && chgrp sinkit /opt/sinkit/ -R && chmod g+s /opt/sinkit/ -R
ADD sinkit.sh /opt/sinkit/
USER sinkit
# Deployment
ADD ear/target/sinkit-ear.ear /opt/sinkit/wildfly-${WILDFLY_VERSION}/standalone/deployments/
CMD ["/opt/sinkit/sinkit.sh"]
