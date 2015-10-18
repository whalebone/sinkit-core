FROM fedora:22
MAINTAINER Michal Karm Babacek <karm@email.com>
LABEL description="Codename Feed: Sinkit Core POC"

ENV DEPS            java-1.8.0-openjdk-devel.x86_64 unzip wget gawk sed
ENV WILDFLY_VERSION 10.0.0.CR3-SNAPSHOT
ENV JBOSS_HOME      "/opt/sinkit/wildfly"
ENV JAVA_HOME       "/usr/lib/jvm/java-1.8.0"

RUN dnf -y update && dnf -y install ${DEPS} && dnf clean all
RUN useradd -s /sbin/nologin sinkit
RUN mkdir -p /opt/sinkit && chown sinkit /opt/sinkit && chgrp sinkit /opt/sinkit && chmod ug+rwxs /opt/sinkit

WORKDIR /opt/sinkit
USER sinkit

# ADD would run every rebuild
#RUN wget http://download.jboss.org/wildfly/${WILDFLY_VERSION}/wildfly-${WILDFLY_VERSION}.zip && \
ADD wildfly-${WILDFLY_VERSION}.zip ./wildfly-${WILDFLY_VERSION}.zip
RUN unzip wildfly-${WILDFLY_VERSION}.zip && rm -rf wildfly-${WILDFLY_VERSION}.zip

# Circle CI builds and tests the archive, so no need for additional checks here.
ADD ear/target/sinkit-ear.ear /opt/sinkit/wildfly-${WILDFLY_VERSION}/standalone/deployments/

# Workaround for https://github.com/docker/docker/issues/5509
RUN ln -s /opt/sinkit/wildfly-${WILDFLY_VERSION} /opt/sinkit/wildfly

# TODO: Fix remove garbage...
EXPOSE 8080/tcp
EXPOSE 46655/udp
EXPOSE 7500/udp
EXPOSE 55200/udp
EXPOSE 54200/udp
EXPOSE 45688/udp
EXPOSE 45700/udp
EXPOSE 7800/udp

ENV WF_CONFIG /opt/sinkit/wildfly/standalone/configuration/standalone-ha.xml

ADD standalone-ha.xml ${WF_CONFIG}

# Yikes, editing an XML file with AWK :-)
RUN awk '{ if ( $0 ~ /<inet-address value=/ ) { printf( "%s\n%s\n", $0, "        <nic name=\"@SINKITNIC@\"/>"); } else {print $0; } }' \
   ${WF_CONFIG} > ${WF_CONFIG}.tmp && mv ${WF_CONFIG}.tmp ${WF_CONFIG}

RUN awk '/periodic-rotating-file-handler/ {f=1} !f; /\/periodic-rotating-file-handler/ {print "<size-rotating-file-handler name=\"FILE\" autoflush=\"true\"><file relative-to=\"jboss.server.log.dir\" path=\"server.log\"/><rotate-size value=\"500M\"/><max-backup-index value=\"4\"/><level name=\"DEBUG\"/></size-rotating-file-handler>"; f=0}' \
${WF_CONFIG} > ${WF_CONFIG}.tmp && mv ${WF_CONFIG}.tmp ${WF_CONFIG}

RUN echo 'JAVA_OPTS="\
 -server \
 -XX:+UseCompressedOops \
 -Xms${SINKIT_MS_RAM:-6144m} \
 -Xmx${SINKIT_MX_RAM:-6144m} \
 -XX:+HeapDumpOnOutOfMemoryError \
 -XX:HeapDumpPath=/opt/sinkit \
 -XX:+UseConcMarkSweepGC \
"' >> /opt/sinkit/wildfly/bin/standalone.conf
ADD sinkit.sh /opt/sinkit/
CMD ["/opt/sinkit/sinkit.sh"]
