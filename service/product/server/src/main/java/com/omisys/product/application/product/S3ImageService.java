package com.omisys.product.application.product;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "S3ImageService")
public class S3ImageService implements ImageService {

    private final AmazonS3 s3Client;

    @Value("${aws.s3.bucket-name.product-origin}")
    private String originBucketName;

    @Value("${aws.s3.bucket-name.product-detail}")
    private String detailBucketName;

    @Override
    public String uploadImage(String type, MultipartFile file) throws IOException {
        String fileName = generateFileName(Objects.requireNonNull(file.getOriginalFilename()));
        log.info("fileName: {}", fileName);
        ObjectMetadata metadata = buildMetadata(file);
        if (type.equals("origin")) {
            s3Client.putObject(originBucketName, fileName, file.getInputStream(), metadata);
            return s3Client.getUrl(originBucketName, fileName).toString();
        } else if (type.equals("detail")) {
            s3Client.putObject(detailBucketName, fileName, file.getInputStream(), metadata);
            return s3Client.getUrl(detailBucketName, fileName).toString();
        }
        return null;
    }

    /**
     * 메타데이터 없이 올리면 S3 가 application/octet-stream 으로 저장해 브라우저가 이미지를
     * 표시하지 않고 내려받는다. 길이를 함께 넘겨야 SDK 가 스트림을 통째로 버퍼링하지 않는다.
     */
    private ObjectMetadata buildMetadata(MultipartFile file) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            metadata.setContentType(contentType);
        }
        return metadata;
    }

    @Override
    public String generateFileName(String originName) {
        String extension = originName.substring(originName.lastIndexOf("."));
        return UUID.randomUUID() + extension;
    }

    @Override
    public void deleteImage(String imgUrl) {
        String[] parts = imgUrl.split("/");
        String bucketName = parts[2].split("\\.")[0];
        String fileName = parts[parts.length - 1];
        s3Client.deleteObject(bucketName, fileName);
    }
}
