FROM fedora:22
MAINTAINER Michal Karm Babacek <karm@redhat.com>
LABEL description="Codename Feed: Sinkit Core POC"

ENV DEPS            java-1.8.0-openjdk-devel.x86_64 unzip wget
ENV WILDFLY_VERSION 9.0.1.Final
ENV JBOSS_HOME      "/opt/sinkit/wildfly-${WILDFLY_VERSION}"
ENV JAVA_HOME       "/usr/lib/jvm/java-1.8.0"

RUN dnf -y update && dnf -y install ${DEPS} && dnf clean all
RUN useradd -s /sbin/nologin sinkit
RUN mkdir -p /opt/sinkit && chown sinkit /opt/sinkit && chgrp sinkit /opt/sinkit && chmod ug+rwxs /opt/sinkit

EXPOSE 8080/tcp
#TBD Infinispan, TBD management...

WORKDIR /opt/sinkit
USER sinkit

# ADD would run every rebuild
RUN wget http://download.jboss.org/wildfly/${WILDFLY_VERSION}/wildfly-${WILDFLY_VERSION}.zip && \
    unzip wildfly-${WILDFLY_VERSION}.zip && rm -rf wildfly-${WILDFLY_VERSION}.zip

# Circle CI builds and tests the archive, so no need for additional checks here.
ADD ear/target/sinkit-ear.ear /opt/sinkit/wildfly-${WILDFLY_VERSION}/standalone/deployments/

# Workaround for https://github.com/docker/docker/issues/5509
RUN ln -s /opt/sinkit/wildfly-${WILDFLY_VERSION} /opt/sinkit/wildfly

CMD ["/opt/sinkit/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
