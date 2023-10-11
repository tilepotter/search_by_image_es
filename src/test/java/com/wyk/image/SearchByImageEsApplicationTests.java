package com.wyk.image;

import com.wyk.image.service.ElasticSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class SearchByImageEsApplicationTests {

    @Resource
    private ElasticSearchService service;

    @Test
    void contextLoads() {

    }

}
