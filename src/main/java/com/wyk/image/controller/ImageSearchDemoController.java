package com.wyk.image.controller;

import com.wyk.image.common.Result;
import com.wyk.image.common.ResultUtils;
import com.wyk.image.service.ElasticSearchService;
import com.wyk.image.task.ExtractImageFeatureVector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * @author wangyingkang
 * @version 1.0
 * @date 2023/9/7 10:05
 * @Description
 */
@Slf4j
@RestController
public class ImageSearchDemoController {

    @Resource
    private ElasticSearchService elasticSearchService;

    @Resource
    private ExtractImageFeatureVector extractImageFeatureVector;

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

    /**
     * 抽取指定路径下图片集的特征向量，将图片存储到 OSS，图片的特征向量存储到 ES
     *
     * @param imagesSetPath 图片集路径
     */
    @GetMapping("/extractImage")
    public Result<Map<String, Object>> extractImage(@RequestParam(value = "imagesSetPath") String imagesSetPath) throws UnsupportedEncodingException {
        return ResultUtils.success(extractImageFeatureVector.extractImgFeatureVector(imagesSetPath.replace("\\", "///")));
    }
}
