package com.wyk.image.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @author wangyingkang
 * @version 1.0
 * @date 2023/9/6 17:11
 * @Description
 */
@Data
public class ImageSearchBean {


    @JsonProperty(value = "file_name")
    private String fileName;

    @JsonProperty(value = "feature_vector")
    private List<Float> featureVector;
}
