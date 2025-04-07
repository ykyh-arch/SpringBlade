# 网络问题无法下载可以改为阿里云镜像
# FROM registry.cn-hangzhou.aliyuncs.com/bladex-repo/alpine-java:openjdk17_cn_slim
FROM bladex/alpine-java:openjdk17_cn_slim

MAINTAINER bladejava@qq.com

RUN mkdir -p /blade/auth

WORKDIR /blade/auth

EXPOSE 8010

ADD ./target/blade-auth.jar ./app.jar

# --add-opens 参数（用于模块化系统，Java 9+），作用：在 Java 9 及以上版本，模块系统（JPMS）默认会限制反射访问某些包。这些参数允许无限制地反射访问 java.lang 和 java.lang.reflect 包。
# -Djava.security.egd=file:/dev/./urandom，作用：设置 JVM 的随机数生成源为 /dev/urandom（非阻塞式随机数生成器）。
# 适用场景：
# 在 Linux 环境下，Java 默认使用 /dev/random（阻塞式），可能导致应用启动变慢（尤其是在容器中）。
# 使用 /dev/urandom 可以加快启动速度（牺牲少量安全性，但对大多数应用无影响）。
ENTRYPOINT ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

CMD ["--spring.profiles.active=test"]
