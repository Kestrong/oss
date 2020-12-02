java ${JAVA_OPTS} \
-XX:+UseConcMarkSweepGC \
-XX:+PrintGCDetails \
-XX:+PrintHeapAtGC \
-XX:+PrintGCDateStamps \
-XX:+PrintTenuringDistribution \
-verbose:gc -Xloggc:/logs/oss-api-gc.log \
-Djava.security.egd=file:/dev/./urandom \
-jar /app.jar