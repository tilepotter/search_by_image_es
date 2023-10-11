package com.wyk.image.service;


import com.wyk.image.bean.ImageSearchBean;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * @author wangyingkang
 * @version 1.0
 * @date 2023/9/6 16:33
 * @Description
 */
public interface ElasticSearchService {


    /**
     * 根据图片特征向量实现以图搜图
     *
     * @param file     图片文件
     * @param score    最小分值
     * @param page     页码
     * @param pageSize 每页条数
     * @return map
     */
    Map<String, Object> imageSearch(MultipartFile file, Float score, Integer page, Integer pageSize) throws IOException;
}
