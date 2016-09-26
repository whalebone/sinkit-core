FROM fedora:24
MAINTAINER Michal Karm Babacek <karm@email.cz
LABEL description="Codename Feed: Sinkit Core POC"

ENV DEPS            java-1.8.0-openjdk-devel.x86_64 unzip wget gawk sed jna.x86_64 jsch-agent-proxy-usocket-jna.noarch
ENV JBOSS_HOME      "/opt/sinkit/wildfly"
ENV JAVA_HOME       "/usr/lib/jvm/java-1.8.0"

RUN dnf -y update && dnf -y install ${DEPS} && dnf clean all
RUN useradd -s /sbin/nologin sinkit
RUN mkdir -p /opt/sinkit && chown sinkit /opt/sinkit && chgrp sinkit /opt/sinkit && chmod ug+rwxs /opt/sinkit

WORKDIR /opt/sinkit
USER sinkit

EXPOSE 8080/tcp
EXPOSE 8009/tcp
EXPOSE 8443/tcp

EXPOSE 4712-4713/tcp

EXPOSE 57600/tcp
EXPOSE 55200/tcp
EXPOSE 54200/tcp
EXPOSE 45700/tcp
EXPOSE 45688/tcp

EXPOSE 55200/udp
EXPOSE 54200/udp
EXPOSE 45688/udp

EXPOSE 7600-7620/tcp
EXPOSE 7600-7620/udp

ENV WILDFLY_VERSION 10.1.0.Final
ENV INFINISPAN_VERSION 8.2.4.Final
ENV WF_CONFIG /opt/sinkit/wildfly/standalone/configuration/standalone-ha.xml
ENV MODORG modules/system/layers/base/org
# ADD wildfly-${WILDFLY_VERSION}.zip ./wildfly-${WILDFLY_VERSION}.zip
RUN \
# Fetch Wildfly and Infinispan Server
    wget http://download.jboss.org/wildfly/${WILDFLY_VERSION}/wildfly-${WILDFLY_VERSION}.zip && \
    unzip wildfly-${WILDFLY_VERSION}.zip && rm -rf wildfly-${WILDFLY_VERSION}.zip && \
    wget http://downloads.jboss.org/infinispan/${INFINISPAN_VERSION}/infinispan-server-${INFINISPAN_VERSION}-bin.zip && \
    unzip infinispan-server-${INFINISPAN_VERSION}-bin.zip && rm -rf infinispan-server-${INFINISPAN_VERSION}-bin.zip && \
# Infinispan query module (not present in Wildfly by default)
    cp infinispan-server-${INFINISPAN_VERSION}/${MODORG}/infinispan/query            /opt/sinkit/wildfly-${WILDFLY_VERSION}/${MODORG}/infinispan/ -v -R && \
    cp infinispan-server-${INFINISPAN_VERSION}/${MODORG}/hibernate/hql               /opt/sinkit/wildfly-${WILDFLY_VERSION}/${MODORG}/hibernate/ -v -R && \
    cp infinispan-server-${INFINISPAN_VERSION}/${MODORG}/antlr/antlr-runtime         /opt/sinkit/wildfly-${WILDFLY_VERSION}/${MODORG}/antlr/ -v -R && \
    cp infinispan-server-${INFINISPAN_VERSION}/${MODORG}/infinispan/objectfilter     /opt/sinkit/wildfly-${WILDFLY_VERSION}/${MODORG}/infinispan/ -v -R && \
    cp infinispan-server-${INFINISPAN_VERSION}/${MODORG}/infinispan/protostream      /opt/sinkit/wildfly-${WILDFLY_VERSION}/${MODORG}/infinispan/ -v -R && \
    cp infinispan-server-${INFINISPAN_VERSION}/${MODORG}/infinispan/hibernate-search /opt/sinkit/wildfly-${WILDFLY_VERSION}/${MODORG}/infinispan/ -v -R && \
    cp infinispan-server-${INFINISPAN_VERSION}/${MODORG}/infinispan/lucene-directory /opt/sinkit/wildfly-${WILDFLY_VERSION}/${MODORG}/infinispan/ -v -R && \
# Infinispan query module dependencies loading workaround
    sed -i 's/<module name="org.infinispan"\/>/<module name="org.infinispan"\/>\n        <module name="org.infinispan.commons"\/>/g' \
        /opt/sinkit/wildfly-${WILDFLY_VERSION}/${MODORG}/infinispan/query/main/module.xml && \
# Hibernate Search mayhem
# Wildfly ignores compatiblity with Infinispan where Indexing is concerned
    rm -v /opt/sinkit/wildfly-${WILDFLY_VERSION}/${MODORG}/hibernate/search/engine/main/hibernate-search-engine-*.jar && \
    hibernatereplace=`cp infinispan-server-${INFINISPAN_VERSION}/${MODORG}/hibernate/search/engine/main/hibernate-search-engine-*.jar \
        /opt/sinkit/wildfly-${WILDFLY_VERSION}/${MODORG}/hibernate/search/engine/main/ -v | \
        sed 's/.*\(hibernate-search-engine-.*.jar\).*/\1/g'` && \
    sed -i "s/hibernate-search-engine-.*.jar/$hibernatereplace/g" /opt/sinkit/wildfly-${WILDFLY_VERSION}/${MODORG}/hibernate/search/engine/main/module.xml && \
# Lucene
    rm -rf /opt/sinkit/wildfly-${WILDFLY_VERSION}/${MODORG}/apache/lucene && \
    cp infinispan-server-${INFINISPAN_VERSION}/${MODORG}/apache/lucene /opt/sinkit/wildfly-${WILDFLY_VERSION}/${MODORG}/apache/ -v -R && \
# Workaround for https://github.com/docker/docker/issues/5509
    ln -s /opt/sinkit/wildfly-${WILDFLY_VERSION} /opt/sinkit/wildfly && \
# Cleanup
    rm -rf infinispan-server-${INFINISPAN_VERSION}

ADD standalone-ha.xml ${WF_CONFIG}

RUN echo 'JAVA_OPTS="\
 -server \
 -Xms${SINKIT_MS_RAM:-6g} \
 -Xmx${SINKIT_MX_RAM:-6g} \
 -XX:MetaspaceSize=128M \
 -XX:MaxMetaspaceSize=512m \
 -XX:+UseG1GC \
 -XX:MaxGCPauseMillis=200 \
 -XX:+HeapDumpOnOutOfMemoryError \
 -XX:HeapDumpPath=/opt/sinkit \
"' >> /opt/sinkit/wildfly/bin/standalone.conf
RUN mkdir -p /opt/sinkit/wildfly/standalone/log/
ADD sinkit.sh /opt/sinkit/
CMD ["/opt/sinkit/sinkit.sh"]

# Deployment
ADD ear/target/sinkit-ear.ear /opt/sinkit/wildfly-${WILDFLY_VERSION}/standalone/deployments/
