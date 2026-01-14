package com.project.cadence.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.project.cadence.dto.s3.FileUploadResult;
import jakarta.servlet.http.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
@RequiredArgsConstructor
public class AwsService {

    private final AmazonS3 amazonS3;
    private final JdbcTemplate jdbcTemplate;


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
    public String getPresignedUrl(String category, String subCategory, @NotNull String name, String extension) {
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

    public S3Object getObject(String fileName) {
        return amazonS3.getObject(s3BucketName, fileName);
    }

    public URL getLink(String fileName) {
        return amazonS3.getUrl(s3BucketName, fileName);
    }

    @Async
    public String uploadBase64File(String base64Data, String category, String subCategory, String fileName, String contentType) {
        if (base64Data.startsWith("data:")) {
            // Remove "data:image/png;base64," part
            base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
        }

        // Decode Base64 string
        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);

        // Create InputStream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(decodedBytes);

        // Metadata
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(decodedBytes.length);
        metadata.setContentType(contentType);

        // Construct S3 key
        String s3Key = category + "/" + subCategory + "/" + fileName.replaceAll("\\s+", "_");

        // Upload to S3
        amazonS3.putObject(s3BucketName, s3Key, inputStream, metadata);

        // Return file URL
        return amazonS3.getUrl(s3BucketName, s3Key).toString();
    }

    public List<FileUploadResult> save(List<Part> parts) {
        try {
            List<CompletableFuture<FileUploadResult>> futures = new ArrayList<>();

            for (Part part : parts) {
                CompletableFuture<FileUploadResult> future = uploadFileAsync(part);
                if (future == null) {
                    return null;
                }
                futures.add(future); // async call returns CompletableFuture
            }

            // Wait for all uploads to complete
            List<FileUploadResult> uploadResults = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();

            for (FileUploadResult result : uploadResults) {
                // Dynamic SQL to update the column with the file URL
                String sql = String.format(
                        "UPDATE %s SET %s = ? WHERE id = ?",
                        result.tableName(),
                        result.columnName()
                );

                jdbcTemplate.update(sql, result.url(), result.primaryKey());
                log.info("Updated table '{}' column '{}' for pk '{}' with URL '{}'",
                        result.tableName(),
                        result.columnName(),
                        result.primaryKey(),
                        result.url());
            }

            return uploadResults;
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
        }
        return null;
    }

    @Async
    public CompletableFuture<FileUploadResult> uploadFileAsync(Part part) {
        try {
            String submittedName = part.getSubmittedFileName();
            String[] nameParts = submittedName.split(" ");

            if (nameParts.length != 4) {
                throw new IllegalArgumentException("Invalid file naming format");
            }

            String tableName = nameParts[0];
            String columnName = nameParts[1];
            String primaryKey = nameParts[2];
            String extension = nameParts[3];

            String objectKey = tableName + "/" + columnName + "/" + primaryKey + "." + extension;

            /* --------- CHECK & DELETE EXISTING FILE --------- */
            if (amazonS3.doesObjectExist(s3BucketName, objectKey)) {
                amazonS3.deleteObject(s3BucketName, objectKey);
                log.info("Existing file deleted: {}/{}", s3BucketName, objectKey);
            }

            /* --------- UPLOAD NEW FILE --------- */
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(part.getSize());
            metadata.setContentType(part.getContentType());

            amazonS3.putObject(
                    s3BucketName,
                    objectKey,
                    part.getInputStream(),
                    metadata
            );

            log.info("File uploaded successfully: {}/{}", s3BucketName, objectKey);

            String fileUrl = amazonS3.getUrl(s3BucketName, objectKey).toString();

            return CompletableFuture.completedFuture(
                    new FileUploadResult(
                            tableName,
                            columnName,
                            primaryKey,
                            fileUrl
                    )
            );

        } catch (Exception e) {
            log.error("Error uploading file", e);
            return CompletableFuture.failedFuture(e);
        }
    }

}
