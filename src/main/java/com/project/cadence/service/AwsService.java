package com.project.cadence.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;


@Slf4j
@Service
@RequiredArgsConstructor
public class AwsService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String s3BucketName;

    public String generateUrl(String fileName, HttpMethod httpMethod) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MINUTE, 15); // Generated URL will be valid for 15 minutes
        return amazonS3.generatePresignedUrl(s3BucketName, fileName, calendar.getTime(), httpMethod).toString();
    }

    @Async
    public boolean findByName(String fileName) {
        return amazonS3.doesObjectExist(s3BucketName, fileName);
    }


    @Async
    public String save(String category, String subCategory, @NotNull String name, String extension) {
        String fileName = category + "/" + subCategory + "/" + name.replaceAll("\\s+", "_") + extension;
        log.info("Generated file name '{}' for saving in bucket '{}'", fileName, s3BucketName);
        return generateUrl(fileName, HttpMethod.PUT);
    }

    public void deleteObject(String fileName) {
        try {
            amazonS3.deleteObject(s3BucketName, fileName);
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
        }
    }
}
