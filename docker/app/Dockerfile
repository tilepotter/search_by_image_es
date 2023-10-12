#java8 基础镜像
FROM williamyeh/java8

#作者信息
MAINTAINER wyk "wswyk.com@gmail.com"

#拷贝jar
ADD ./jar/searchByImage.jar searchByImage.jar

#暴露端口
EXPOSE 5005

#编写入口ENTRYPOINT；docker启动时,运行的命令
ENTRYPOINT ["java","-jar","searchByImage.jar"]