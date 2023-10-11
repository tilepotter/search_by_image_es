package com.wyk.image.task;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.wyk.image.bean.ImageSearchBean;
import com.wyk.oss.core.OssTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wangyingkang
 * @version 1.0
 * @date 2023/9/20 10:15
 * @Description
 */
@Slf4j
@Component
public class ExtractImageFeatureVector {

    @Resource
    private ElasticsearchClient client;

    @Resource
    private OssTemplate ossTemplate;

    /**
     * oss存储桶名
     */
    @Value("${oss.bucketName}")
    private String bucketName;

    /**
     * VGG 模型url
     */
    @Value("${settings.service.vgg-url}")
    private String vggUrl;

    /**
     * 抽取图片的特征向量写到 ES ，并将图片存储到 OSS
     *
     * @param imagesSetPath 图片集路径
     * @return map -> isSuccess:是否成功，totalImages：图片总量
     */
    public Map<String, Object> extractImgFeatureVector(String imagesSetPath) {
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("isSuccess", false);
        resultMap.put("totalImages", null);
        log.info("--------------提取【{}】下图片特征向量写到es--------------", imagesSetPath);
        File folder = new File(imagesSetPath);
        if (folder.exists() && folder.isDirectory()) {
            //路径下的所有图片
            File[] files = folder.listFiles();
            if (files != null && files.length > 0) {
                List<ImageSearchBean> result = new ArrayList<>(files.length);
                for (File file : files) {
                    //获取图片的 base64 值
                    String encode = Base64.encode(FileUtil.getInputStream(file));
                    HashMap<String, Object> param = new HashMap<>();
                    param.put("img", encode);
                    //调用VGG 提取图片的特征向量
                    String response = HttpUtil.post(vggUrl, JSONUtil.toJsonStr(param, JSONConfig.create().setIgnoreNullValue(false)), 30000);
                    JSONObject jsonObject = JSONUtil.parseObj(response);
                    Integer code = jsonObject.getInt("code");
                    if (code == 200) {
                        log.info("---- 调用VGG 抽取图片【{}】特征向量成功 ----", file.getName());
                        List<Float> data = JSONUtil.toList(jsonObject.getJSONArray("data"), Float.class);

                        //将图片存储到oss
                        try {
                            ossTemplate.putObject(bucketName, file.getName(), FileUtil.getInputStream(file));
                        } catch (Exception e) {
                            log.error("图片上传OSS失败！");
                            e.printStackTrace();
                        }

                        ImageSearchBean imageSearchBean = new ImageSearchBean();
                        imageSearchBean.setFileName(file.getName());
                        imageSearchBean.setFeatureVector(data);
                        result.add(imageSearchBean);
                    } else {
                        log.error("调用VGG失败，msg -> {}", jsonObject.getStr("msg"));
                    }
                }
                //数据写入到es
                if (result.size() > 0) {
                    boolean success = this.saveData2Es(result);
                    resultMap.put("isSuccess", success);
                    resultMap.put("totalImages", result.size());
                }
            }
        } else {
            log.error("读取路径错误，请检查路径是否有误！");
        }
        return resultMap;
    }

    /**
     * 批量写入数据到 es-8.0
     *
     * @param list 数据集
     */
    public boolean saveData2Es(List<ImageSearchBean> list) {
        List<BulkOperation> bulkOperationList = new ArrayList<>();
        for (ImageSearchBean imageSearchBean : list) {
            bulkOperationList.add(BulkOperation.of(o -> o.index(i -> i.document(imageSearchBean))));
        }
        try {
            BulkResponse bulkResponse = client.bulk(b -> b.index("search_by_image_index")
                    .operations(bulkOperationList));
            log.info("---- bulk {}条数据到 es成功，耗时：{} ms ----", list.size(), bulkResponse.took());
            return !bulkResponse.errors();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
