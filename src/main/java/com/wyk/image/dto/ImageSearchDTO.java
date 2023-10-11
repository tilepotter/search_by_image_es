package com.wyk.image.dto;

import lombok.Data;

/**
 * @author wangyingkang
 * @version 1.0
 * @date 2023/9/7 10:32
 * @Description
 */
@Data
public class ImageSearchDTO {

    /**
     * 分值
     */
    private Double score;

    /**
     * 文件名
     */
    private String imageName;

    /**
     * 图片url
     */
    private String imageUrl;

    /**
     * 512位特征向量
     */
    //private List<Float> featureVector;
}
