# 以图搜图Demo

## 1、项目实现了哪些demo功能？

* 特征向量提取：使用`VGG19` 模型提取图片的特征向量
* 特征向量存储：使用 `Elasticsearch-8.0`  `dense_vector`类型存储特征向量
* 检索召回：使用`cosineSimilarity` 余弦相似度函数计算向量之间的相似度得分
* 图片存储：`OSS`对象存储

## 2、demo介绍

这个demo比较简单的使用了预训练模型`VGG19`直接提取图片的特征向量，使用`Flask`封装`VGG`算法，可通过`RESTful HTTP`接口直接调用。项目`VGG`模型方面中直接使用了这位大佬的成品代码，大佬的博客地址及`GitHub`地址：https://www.eula.club/，https://github.com/Logistic98/yoyo-algorithm/tree/master/image-feature-vector

存储和检索方面使用的 ES-8.0 。ES 从7.2版本引入了向量类型dense_vector，开始支持向量类型的存储及检索，但针对海量图片的向量存储，以及后续检索召回，性能及速度没有做过测试。按道理Script做的是暴力检索，随着向量维度的增加和数据量的上升，检索效率及召回率肯定下降，工程上的一些建议是增加query的参数，限制匹配的文档数量，比如时间范围、检索类型等。

最近几年向量存储开源社区比较火的一款向量数据库`Milvus`也是许多工程化落地的优选方案，我这里直接用了个人比较熟悉的`Elasticsearch`，有兴趣的铁子们可以试一下使用`Milvus`。

前端页面鄙人不才，没有去写，都是基于后端接口调用。。。

## 3、快速开始

### 3.1 修改相关配置

- 修改VGG模型后端服务地址
- 修改ES地址（集群用“,”隔开，我这里使用8.0.0版本）
- 修改对象存储地址，我这里使用开源对象存储MinIO，简单易配

### ![image-20231011162240788](https://github.com/tilepotter/search_by_image_es/blob/main/md_img/image-20231011162240788.png?raw=true)



### 3.2 创建索引

使用CURL命令或直接在Kibana上执行创建 mapping 语句：

```shell
curl  -XPUT 'http://localhost:9200/search_by_image_index' -H 'Content-Type: application/json' -d '
 {
	"mappings": {
		"properties": {
			"file_name": {
				"type": "keyword"
			},
			"feature_vector": {
				"type": "dense_vector",
				"dims": 512
			}
		}
	}
 }
'
```

### 3.3 运行模型后端服务：
切换到`image-feature-vector`目录下，安装项目运行所需的python包：
```python
pip install -r requirements.txt 
```
安装较慢以及安装出问题的话可使用阿里云镜像，或者使用代理：
* 阿里云镜像安装
```python
pip install -r requirements.txt -i http://mirrors.aliyun.com/pypi/simple/ --trusted-host mirrors.aliyun.com
```
* 使用代理
```python
pip install -r requirements.txt --proxy http://127.0.0.1:1789
```
运行`Flask`封装的模型Web服务：
```python
python3.8  server.py
```

### 3.4 预加载图片集数据到ES、OSS

#### 3.4.1使用如下接口抽取指定路径下图片集的特征向量，将图片存储到 OSS，图片的特征向量存储到 ES：

```java
 /**
     * 抽取指定路径下图片集的特征向量，将图片存储到 OSS，图片的特征向量存储到 ES
     *
     * @param imagesSetPath 图片集路径
     */
    @GetMapping("/extractImage")
    public Result<Map<String, Object>> extractImage(@RequestParam(value = "imagesSetPath") String imagesSetPath) throws UnsupportedEncodingException {
        return ResultUtils.success(extractImageFeatureVector.extractImgFeatureVector(imagesSetPath.replace("\\", "///")));
    }
```

#### 3.4.2 Postman 调用接口如下：

![image-20231011163544460](https://github.com/tilepotter/search_by_image_es/blob/main/md_img/image-20231011163544460.png?raw=true)

#### 3.4.3后端接口日志：

![image-20231011163448074](https://github.com/tilepotter/search_by_image_es/blob/main/md_img/image-20231011163448074.png?raw=true)



### 3.5 检索召回

#### 3.5.1 检索召回使用如下接口：

```java
/**
     * 根据图片特征向量实现以图搜图
     *
     * @param file     图片文件
     * @param score    相似度值
     * @param page     第几页
     * @param pageSize 分页大小
     * @return result
     */
    @RequestMapping(value = "/searchByImage", method = RequestMethod.POST)
    public Result<Map<String, Object>> searchByImage(@RequestParam MultipartFile file,
                                                     @RequestParam(defaultValue = "0.8") Float score,
                                                     @RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            return ResultUtils.success(elasticSearchService.imageSearch(file, score, page, pageSize));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResultUtils.error(500, ex.getMessage());
        }
    }
```

#### 3.5.2 召回测试；

使用如下`2007_001457.jpg`这张图片进行检索召回测试：

![2007_001457](https://github.com/tilepotter/search_by_image_es/blob/main/md_img/2007_001457.jpg?raw=true)

Postman 调用接口如下：

![image-20231011164509769](https://github.com/tilepotter/search_by_image_es/blob/main/md_img/image-20231011164509769.png?raw=true)

其中score是余弦相似度计算之后的分值，imageName为图片名称，imageUrl为对象存储返回的图片访问临时URL。复制该URL到浏览器访问如下：

![image-20231011164819943](https://github.com/tilepotter/search_by_image_es/blob/main/md_img/image-20231011164819943.png?raw=true)

可以看到同样的图片召回率是100%，后续可使用向量数据库`Milvus`试试效果如何。

## 4.部署
项目demo支持原生部署和Docker容器化部署。
### 4.1 原生部署：
下载项目后配置项目所需的Elasticsearch-8.0、对象存储环境，运行maven打包命令：
```shell
mvn package -Dmaven.test.skip=true  
```
生成部署用的后端服务jar包。
将`image-feature-vector`目录全部上传至服务器，执行`3.3步骤`，运行模型后端Web服务。

### 4.2 Docker部署：
使用docker-compose部署本项目，项目依赖的所有环境、组件都已进行镜像生成：
```dockerfile
version: '3.0'
services:
  # VGG19 模型 server容器
  image-feature-vector:
    #image: image-feature-vector:latest
    # 首次运行时候使用如下命令构建镜像
    container_name: image-feature-vector
    build:
      context: ./image-feature-vector
      dockerfile: Dockerfile
    ports:
      - "5004:5004"
    environment:
      TZ: Asia/Shanghai
    networks:
      app_net:
        ipv4_address: 172.16.238.10

  # ES-8.0
  elasticsearch:
    image: elasticsearch:8.0.0
    container_name: elasticsearch
    ports:
      - "9200:9200"
      - "9300:9300"
    environment:
      # 设置集群名称
      cluster.name: elasticsearch-8.0
      # 以单一节点模式启动
      discovery.type: single-node
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"
      xpack.security.enabled: false
      xpack.security.enrollment.enabled: false
      xpack.security.http.ssl.enabled: false
      xpack.security.transport.ssl.enabled: false
    volumes:
      - ./elasticsearch/plugins:/usr/share/elasticsearch/plugins
      - ./elasticsearch/data:/usr/share/elasticsearch/data
      - ./elasticsearch/logs:/usr/share/elasticsearch/logs
    networks:
      app_net:
        ipv4_address: 172.16.238.11

  #对象存储minio
  minio:
    image: minio/minio:RELEASE.2022-05-26T05-48-41Z
    container_name: minio
    ports:
      # api 端口
      - "9000:9000"
      # 控制台端口
      - "9001:9001"
    environment:
      # 时区上海
      TZ: Asia/Shanghai
      # 管理后台用户名
      MINIO_ROOT_USER: minioadmin
      # 管理后台密码，最小8个字符
      MINIO_ROOT_PASSWORD: minioadmin
      # https需要指定域名
      #MINIO_SERVER_URL: "https://xxx.com:9000"
      #MINIO_BROWSER_REDIRECT_URL: "https://xxx.com:9001"
      # 开启压缩 on 开启 off 关闭
      MINIO_COMPRESS: "off"
      # 扩展名 .pdf,.doc 为空 所有类型均压缩
      MINIO_COMPRESS_EXTENSIONS: ""
      # mime 类型 application/pdf 为空 所有类型均压缩
      MINIO_COMPRESS_MIME_TYPES: ""
    volumes:
      # 映射当前目录下的data目录至容器内/data目录
      - ./minio/data:/data
      # 映射配置目录
      - ./minio/config:/root/.minio/
    command: server --address ':9000' --console-address ':9001' /data  # 指定容器中的目录 /data
    networks:
      app_net:
        ipv4_address: 172.16.238.12

  #应用容器后端服务
  search_by_image:
    #image: search_by_image:latest
    # 首次运行时候使用如下命令构建镜像
    container_name: search_by_image
    build:
      context: ./app
      dockerfile: Dockerfile
    ports:
      - "5005:5005"
    depends_on:
      - image-feature-vector
      - elasticsearch
      - minio
    networks:
      app_net:
        ipv4_address: 172.16.238.13

networks:
  app_net:
    driver: bridge
    ipam:
      driver: default
      config:
        - subnet: 172.16.238.0/24
          gateway: 172.16.238.1
```


