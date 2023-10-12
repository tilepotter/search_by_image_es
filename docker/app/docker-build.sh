#构建镜像
docker build -t wyk/search_by_image:1.0 .

#运行 docker 容器
docker run -d --name search_by_image -p 5005:5005 wyk/search_by_image:1.0