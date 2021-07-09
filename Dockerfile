FROM centos:7

MAINTAINER Eric Löffler <eloeffler@aservo.com>

# force sbt to use a specific repository
ARG MAIN_REPO_REALM
ARG MAIN_REPO_URL
ARG MAIN_REPO_USERNAME
ARG MAIN_REPO_PASSWORD
ARG JVM_ONLY_HTTP_PROXY_HOST
ARG JVM_ONLY_HTTP_PROXY_PORT
ARG JVM_ONLY_HTTPS_PROXY_HOST
ARG JVM_ONLY_HTTPS_PROXY_PORT
ARG JVM_ONLY_NO_PROXY

ADD etc/ /app/etc/
ADD src/ /app/src/
ADD project/build.properties /app/project/
ADD project/plugins.sbt /app/project/
ADD build.sbt /app/
ADD init.sh /app/
ADD start.sh /app/

RUN groupadd -r -g 1000 appuser && \
    useradd -r -g 1000 -u 1000 appuser && \
    mkdir /var/app && \
    chown -R appuser:appuser /app /var/app && \
    chmod a+x /app/start.sh && \
    mkhomedir_helper appuser

RUN yum clean all --enablerepo=* && \
    yum -y install deltarpm \
                   yum-utils \
                   ca-certificates && \
    yum -y update && \                   
    yum -y install epel-release && \
    curl https://www.scala-sbt.org/sbt-rpm.repo | tee > /etc/yum.repos.d/sbt-rpm.repo && \
    yum -y install \
        java-1.8.0-openjdk-headless \
        java-1.8.0-openjdk \
        java-1.8.0-openjdk-devel \
        sbt \
        openssl \
        nc \
        jq && \
    mv /etc/yum.repos.d/sbt-rpm.repo /etc/yum.repos.d/sbt-rpm.repo.disabled && \
    yum clean all --enablerepo=* && \
    rm -rf /tmp/* /var/tmp/*

COPY ca_certs/ /usr/share/pki/ca-trust-source/anchors/

RUN update-ca-trust

ENV JAVA_OPTS "-Dfile.encoding=UTF-8"

ENV SBT_OPTS "--no-colors" \
    "-Dsbt.global.base=/var/app/sbt" \
    "-Dsbt.boot.directory=/var/app/sbt/boot" \
    "-Dsbt.ivy.home=/var/app/ivy2"

WORKDIR /app

USER appuser

RUN if [ -n "$JVM_ONLY_HTTP_PROXY_HOST" ]; then export JAVA_OPTS="-Dhttp.proxyHost=$JVM_ONLY_HTTP_PROXY_HOST $JAVA_OPTS"; fi && \
    if [ -n "$JVM_ONLY_HTTP_PROXY_PORT" ]; then export JAVA_OPTS="-Dhttp.proxyPort=$JVM_ONLY_HTTP_PROXY_PORT $JAVA_OPTS"; fi && \
    if [ -n "$JVM_ONLY_HTTPS_PROXY_HOST" ]; then export JAVA_OPTS="-Dhttps.proxyHost=$JVM_ONLY_HTTPS_PROXY_HOST $JAVA_OPTS"; fi && \
    if [ -n "$JVM_ONLY_HTTPS_PROXY_PORT" ]; then export JAVA_OPTS="-Dhttps.proxyPort=$JVM_ONLY_HTTPS_PROXY_PORT $JAVA_OPTS"; fi && \
    if [ -n "$JVM_ONLY_NO_PROXY" ]; then export JAVA_OPTS="-Dhttp.nonProxyHosts=$JVM_ONLY_NO_PROXY $JAVA_OPTS"; fi && \
    sbt compile

ENTRYPOINT ["/app/init.sh"]
CMD ["/app/start.sh"]
