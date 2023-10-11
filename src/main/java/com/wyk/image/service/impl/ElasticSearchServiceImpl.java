package com.wyk.image.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ScriptLanguage;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.json.JsonData;
import com.wyk.image.bean.ImageSearchBean;
import com.wyk.image.dto.ImageSearchDTO;
import com.wyk.image.service.ElasticSearchService;
import com.wyk.oss.core.OssTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author wangyingkang
 * @version 1.0
 * @date 2023/9/6 17:46
 * @Description
 */
@Slf4j
@Service
public class ElasticSearchServiceImpl implements ElasticSearchService {

    @Value("${settings.service.vgg-url}")
    private String vggUrl;

    @Resource
    private ElasticsearchClient esClient;

    @Resource
    private OssTemplate ossTemplate;

    /**
     * oss存储桶名
     */
    @Value("${oss.bucketName}")
    private String bucketName;

    @Override
    public Map<String, Object> imageSearch(MultipartFile file, Float score, Integer page, Integer pageSize) throws IOException {
        //分页参数预设置
        if (page == null || page.equals(1)) {
            page = 0;
        }
        if (pageSize == null || pageSize.equals(0)) {
            pageSize = 10;
        }
        String base64 = null;
        JSONObject paramObject = new JSONObject();
        try {
            base64 = Base64.encode(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            log.error("上传图片转base64错误！");
        }
        if (ObjectUtil.isNotEmpty(base64)) {
            paramObject.set("img", base64);
        }

        String vggResult = "";
        try {
            vggResult = HttpUtil.post(vggUrl, paramObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("获取上传图片的特征向量错误!");
        }

               /* Script script = new Script(Script.DEFAULT_SCRIPT_TYPE, "painless", "cosineSimilarity(params.queryVector,'feature_vector')", esParams);
                // 构建查询对象
                ScriptScoreQueryBuilder scriptScoreQueryBuilder = new ScriptScoreQueryBuilder(QueryBuilders.existsQuery("feature_vector"), script);
                NativeSearchQueryBuilder query = new NativeSearchQueryBuilder()
                        .withMinScore(score)
                        .withPageable(PageRequest.of(page - 1, pageSize))
                        .withQuery(scriptScoreQueryBuilder);
                search = elasticsearchTemplate.search(query.build(), ImageSearchBean.class);*/
        JSONObject jsonObject = JSONUtil.parseObj(vggResult);
        JSONArray featureVector = jsonObject.getJSONArray("data");
        //es查询条件参数
        Map<String, JsonData> esParams = new HashMap<>();
        esParams.put("queryVector", JsonData.of(featureVector));

        //es 使用JAVA API 进行相似度检索
        Integer finalPage = page;
        Integer finalPageSize = pageSize;
        SearchResponse<ImageSearchBean> searchResponse = esClient.search(s -> s
                .index("search_by_image_index")
                .query(q -> q
                        .scriptScore(l -> l
                                .query(query -> query.matchAll(matchAll -> matchAll))
                                .script(sc -> sc
                                        .inline(i -> i
                                                .lang(ScriptLanguage.Painless)
                                                .source("cosineSimilarity(params.queryVector,'feature_vector')")
                                                .params(esParams)))))
                .from(finalPage)
                .size(finalPageSize)
                .minScore(Double.valueOf(score)) //最小分值
                .sort(d -> d
                        .field(f -> f
                                .field("_score")
                                .order(SortOrder.Desc))
                ), ImageSearchBean.class);

        Map<String, Object> result = new HashMap<>();
        HitsMetadata<ImageSearchBean> hitsMetadata = searchResponse.hits();
        long total = hitsMetadata.total() != null ? hitsMetadata.total().value() : 0;

        List<ImageSearchDTO> resultList = hitsMetadata.hits().stream().map(hit -> {
            ImageSearchDTO imageSearchDTO = new ImageSearchDTO();
            imageSearchDTO.setScore(hit.score());
            if (hit.source() != null) {
                //获取图片在OSS上的临时URL
                String objectURL = ossTemplate.getObjectURL(bucketName, hit.source().getFileName(), 1);
                imageSearchDTO.setImageName(hit.source().getFileName());
                imageSearchDTO.setImageUrl(objectURL);
            }
            return imageSearchDTO;
        }).collect(Collectors.toList());

        result.put("total", total);
        result.put("data", resultList);
        return result;
    }
}
