FROM fedora:22
MAINTAINER Michal Karm Babacek <karm@email.com>
LABEL description="Codename Feed: Sinkit Core POC"

ENV DEPS            java-1.8.0-openjdk-devel.x86_64 unzip wget gawk sed
ENV JBOSS_HOME      "/opt/sinkit/wildfly"
ENV JAVA_HOME       "/usr/lib/jvm/java-1.8.0"

RUN dnf -y update && dnf -y install ${DEPS} && dnf clean all
RUN useradd -s /sbin/nologin sinkit
RUN mkdir -p /opt/sinkit && chown sinkit /opt/sinkit && chgrp sinkit /opt/sinkit && chmod ug+rwxs /opt/sinkit

WORKDIR /opt/sinkit
USER sinkit

ENV WILDFLY_VERSION 10.0.0.CR5-SNAPSHOT
ENV HIBERNATE_HQL_LUCENE_VERSION 1.3.0.Alpha2
ENV HIBERNATE_HQL_PARSER_VERSION 1.3.0.Alpha2
ENV STRINGTEMPLATE_VERSION 3.2.1
ENV ANTLR_RUNTIME_VERSION 3.4
ENV VERSION_INFINISPAN 8.1.0.Final
ENV MAVEN_CENTRAL http://central.maven.org/maven2

# ADD would run every rebuild
#RUN wget http://download.jboss.org/wildfly/${WILDFLY_VERSION}/wildfly-${WILDFLY_VERSION}.zip && \

# For the time being, we run with latest builds
# wget https://github.com/wildfly/wildfly/archive/master.zip
# unzip and build with ./build.sh -DskipTests :-)
ADD wildfly-${WILDFLY_VERSION}.zip ./wildfly-${WILDFLY_VERSION}.zip
RUN unzip wildfly-${WILDFLY_VERSION}.zip && rm -rf wildfly-${WILDFLY_VERSION}.zip

# Infinispan query module
ENV INFINISPAN_MODULE_DIR /opt/sinkit/wildfly-${WILDFLY_VERSION}/modules/system/layers/base/org/infinispan/query/main/
RUN mkdir -p ${INFINISPAN_MODULE_DIR}
ADD module.xml ${INFINISPAN_MODULE_DIR}/module.xml
RUN sed -i "s/@HIBERNATE_HQL_LUCENE_VERSION@/${HIBERNATE_HQL_LUCENE_VERSION}/g" ${INFINISPAN_MODULE_DIR}/module.xml && \
sed -i "s/@HIBERNATE_HQL_PARSER_VERSION@/${HIBERNATE_HQL_PARSER_VERSION}/g" ${INFINISPAN_MODULE_DIR}/module.xml && \
sed -i "s/@STRINGTEMPLATE_VERSION@/${STRINGTEMPLATE_VERSION}/g" ${INFINISPAN_MODULE_DIR}/module.xml && \
sed -i "s/@ANTLR_RUNTIME_VERSION@/${ANTLR_RUNTIME_VERSION}/g" ${INFINISPAN_MODULE_DIR}/module.xml && \
sed -i "s/@VERSION_INFINISPAN@/${VERSION_INFINISPAN}/g" ${INFINISPAN_MODULE_DIR}/module.xml && \
wget ${MAVEN_CENTRAL}/org/hibernate/hql/hibernate-hql-lucene/${HIBERNATE_HQL_LUCENE_VERSION}/hibernate-hql-lucene-${HIBERNATE_HQL_LUCENE_VERSION}.jar -O ${INFINISPAN_MODULE_DIR}/hibernate-hql-lucene-${HIBERNATE_HQL_LUCENE_VERSION}.jar && \
wget ${MAVEN_CENTRAL}/org/hibernate/hql/hibernate-hql-parser/${HIBERNATE_HQL_PARSER_VERSION}/hibernate-hql-parser-${HIBERNATE_HQL_PARSER_VERSION}.jar -O ${INFINISPAN_MODULE_DIR}/hibernate-hql-parser-${HIBERNATE_HQL_PARSER_VERSION}.jar && \
wget ${MAVEN_CENTRAL}/org/antlr/stringtemplate/${STRINGTEMPLATE_VERSION}/stringtemplate-${STRINGTEMPLATE_VERSION}.jar -O ${INFINISPAN_MODULE_DIR}/stringtemplate-${STRINGTEMPLATE_VERSION}.jar && \
wget ${MAVEN_CENTRAL}/org/antlr/antlr-runtime/${ANTLR_RUNTIME_VERSION}/antlr-runtime-${ANTLR_RUNTIME_VERSION}.jar -O ${INFINISPAN_MODULE_DIR}/antlr-runtime-${ANTLR_RUNTIME_VERSION}.jar && \
wget ${MAVEN_CENTRAL}/org/infinispan/infinispan-directory-provider/${VERSION_INFINISPAN}/infinispan-directory-provider-${VERSION_INFINISPAN}.jar -O ${INFINISPAN_MODULE_DIR}/infinispan-directory-provider-${VERSION_INFINISPAN}.jar && \
wget ${MAVEN_CENTRAL}/org/infinispan/infinispan-lucene-directory/${VERSION_INFINISPAN}/infinispan-lucene-directory-${VERSION_INFINISPAN}.jar -O ${INFINISPAN_MODULE_DIR}/infinispan-lucene-directory-${VERSION_INFINISPAN}.jar && \
wget ${MAVEN_CENTRAL}/org/infinispan/infinispan-query-dsl/${VERSION_INFINISPAN}/infinispan-query-dsl-${VERSION_INFINISPAN}.jar -O ${INFINISPAN_MODULE_DIR}/infinispan-query-dsl-${VERSION_INFINISPAN}.jar && \
wget ${MAVEN_CENTRAL}/org/infinispan/infinispan-objectfilter/${VERSION_INFINISPAN}/infinispan-objectfilter-${VERSION_INFINISPAN}.jar -O ${INFINISPAN_MODULE_DIR}/infinispan-objectfilter-${VERSION_INFINISPAN}.jar && \
wget ${MAVEN_CENTRAL}/org/infinispan/infinispan-query/${VERSION_INFINISPAN}/infinispan-query-${VERSION_INFINISPAN}.jar -O ${INFINISPAN_MODULE_DIR}/infinispan-query-${VERSION_INFINISPAN}.jar

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

RUN echo 'JAVA_OPTS="\
 -server \
 -Xms${SINKIT_MS_RAM:-6g} \
 -Xmx${SINKIT_MX_RAM:-6g} \
 -XX:+UseLargePages \
 -XX:LargePageSizeInBytes=2m \
 -XX:+UseConcMarkSweepGC \
 -XX:+HeapDumpOnOutOfMemoryError \
 -XX:HeapDumpPath=/opt/sinkit \
 -verbose:gc \
 -Xloggc:"/opt/sinkit/wildfly/standalone/log/gc.log" \
 -XX:+PrintGCDetails \
 -XX:+PrintGCDateStamps \
 -XX:+UseGCLogFileRotation \
 -XX:NumberOfGCLogFiles=5 \
 -XX:GCLogFileSize=200M \
 -XX:-TraceClassUnloading \
"' >> /opt/sinkit/wildfly/bin/standalone.conf
RUN mkdir -p /opt/sinkit/wildfly/standalone/log/
ADD sinkit.sh /opt/sinkit/
CMD ["/opt/sinkit/sinkit.sh"]
