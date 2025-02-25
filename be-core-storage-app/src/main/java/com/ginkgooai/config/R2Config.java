package com.ginkgooai.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: david
 * @date: 23:09 2025/2/8
 */

@Configuration
public class R2Config {

    @Value("${CLOUDFLARE_R2_ACCESS_ID:}")
    private String accessId;

    @Value("${CLOUDFLARE_R2_SECRET_KEY:}")
    private String secretKey;

    @Value("${CLOUDFLARE_R2_ENDPOINTS:}")
    private String endpoints;


    @Bean
    public AmazonS3 s3Client() {
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AmazonS3ClientBuilder.EndpointConfiguration(endpoints, "auto"))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessId, secretKey)))
                .withPathStyleAccessEnabled(true)  // Cloudflare R2 需要路径样式访问
                .build();
    }

}
