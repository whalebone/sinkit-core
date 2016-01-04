#!/bin/bash

# @author Tomas Kozel

export HIBERNATE_HQL_LUCENE_VERSION=1.3.0.Alpha2
export HIBERNATE_HQL_PARSER_VERSION=1.3.0.Alpha2
export STRINGTEMPLATE_VERSION=3.2.1
export ANTLR_RUNTIME_VERSION=3.4
export VERSION_INFINISPAN=8.1.0.Final
export MAVEN_CENTRAL=http://central.maven.org/maven2
export SINKIT_HOME=$PWD

#git clone https://github.com/wildfly/wildfly.git
#cd wildfly && mvn install -DskipTests
#cp build/target/wildfly-${WILDFLY_VERSION} ${SINKIT_HOME}/
wget http://download.jboss.org/wildfly/${WILDFLY_VERSION}/wildfly-${WILDFLY_VERSION}.zip && unzip wildfly-${WILDFLY_VERSION}.zip
mv wildfly-${WILDFLY_VERSION} ${JBOSS_HOME}
cp ${SINKIT_HOME}/standalone-ha.xml ${JBOSS_HOME}/standalone/configuration/standalone-ha.xml

export INFINISPAN_MODULE_DIR=${JBOSS_HOME}/modules/system/layers/base/org/infinispan/query/main/
mkdir -p ${INFINISPAN_MODULE_DIR}
cp ${SINKIT_HOME}/module.xml ${INFINISPAN_MODULE_DIR}/module.xml
sed -i "s/@HIBERNATE_HQL_LUCENE_VERSION@/${HIBERNATE_HQL_LUCENE_VERSION}/g" ${INFINISPAN_MODULE_DIR}/module.xml && \
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

export WF_CONFIG=${JBOSS_HOME}/standalone/configuration/standalone-ha.xml
cp ${SINKIT_HOME}/standalone-ha.xml ${WF_CONFIG}

echo 'JAVA_OPTS="\
 -server \
 -XX:+UseCompressedOops \
 -Xms${SINKIT_MS_RAM:-64m} \
 -Xmx${SINKIT_MX_RAM:-512m} \
 -XX:+HeapDumpOnOutOfMemoryError \
 -XX:HeapDumpPath=/opt/sinkit \
 -XX:+UseConcMarkSweepGC \
"' >> ${JBOSS_HOME}/bin/standalone.conf

sed -i "s/@SINKITNIC@/lo/g" ${WF_CONFIG}
sed -i "s/@SINKITLOGGING@/INFO/g" ${WF_CONFIG}
sed -i "s/<core-environment>/<core-environment node-identifier=\"circle-node\">/g" ${WF_CONFIG}

