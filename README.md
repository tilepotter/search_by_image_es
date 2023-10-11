# 以图搜图Demo

## 1、项目实现了哪些demo功能？

* 特征向量提取：使用`VGG19` 模型提取图片的特征向量
* 特征向量存储：使用 `Elasticsearch-8.0`  `dense_vector`类型存储特征向量
* 检索召回：使用`cosineSimilarity` 余弦相似度函数计算向量之间的相似度得分
* 图片存储：`OSS`对象存储

## 2、为什么会写这个demo？

本人之前对以图搜图学习了许久，研究学习了一些以图搜图工程化落地的实现方法，总想着动手试一试，难点在于非结构化数据的特征向量提取、存储和检索召回。这个demo比较简单的使用了预训练模型`VGG19`直接提取图片的特征向量，使用`Flask`封装`VGG`算法。项目`VGG`模型方面中直接使用了这位大佬的成品代码，大佬的博客地址及`GitHub`地址：https://www.eula.club/，https://github.com/Logistic98/yoyo-algorithm/tree/master/image-feature-vector

存储和检索方面使用的 ES-8.0 。ES 从7.2版本引入了向量类型dense_vector，开始支持向量类型的存储及检索，但针对海量图片的向量存储，以及后续检索召回，性能及速度没有做过测试。按道理Script做的是暴力检索，随着向量维度的增加和数据量的上升，检索效率及召回率肯定下降，工程上的一些建议是增加query的参数，限制匹配的文档数量，比如时间范围、检索类型等。

最近几年向量存储开源社区比较火的一款向量数据库`Milvus`也是许多工程化落地的优选方案，我这里直接用了个人比较熟悉的`Elasticsearch`，有兴趣的铁子们可以试一下使用`Milvus`。

前端页面鄙人不才，没有去写，都是基于后端接口调用。。。

## 3、快速开始

### 3.1 修改相关配置

- 修改VGG模型后端服务地址
- 修改ES地址（集群用“,”隔开，我这里使用8.0.0版本）
- 修改对象存储地址，我这里使用开源对象存储MinIO，简单易配

### ![image-20231011162240788](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20231011162240788.png)



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



### 3.3 预加载图片集数据到ES、OSS

#### 3.3.1使用如下接口抽取指定路径下图片集的特征向量，将图片存储到 OSS，图片的特征向量存储到 ES：

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

#### 3.3.2 Postman 调用接口如下：

![image-20231011163544460](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20231011163544460.png)

#### 3.3.3后端接口日志：

![image-20231011163448074](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20231011163448074.png)



### 3.4 检索召回

#### 3.4.1 检索召回使用如下接口：

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

#### 3.4.2 召回测试；

使用如下`2007_001457.jpg`这张图片进行检索召回测试：

![2007_001457](D:\Download\Train_Images_Set\set01_500\2007_001457.jpg)

Postman 调用接口如下：

![image-20231011164509769](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20231011164509769.png)

其中score是余弦相似度计算之后的分值，imageName为图片名称，imageUrl为对象存储返回的图片访问临时URL。复制该URL到浏览器访问如下：

![image-20231011164819943](C:\Users\lenovo\AppData\Roaming\Typora\typora-user-images\image-20231011164819943.png)

可以看到同样的图片召回率是100%，后续可使用向量数据库`Milvus`试试效果如何。