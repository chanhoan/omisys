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

/**
 * 상품 이미지를 버킷 하나에 프리픽스로 나눠 담고, 조회용 URL 은 CloudFront 를 가리키게 한다.
 *
 * <p>버킷을 종류별로 쪼개 두면 URL 에서 버킷명을 문자열로 갈아끼우는 코드가 생기고, 그런 코드는
 * 조용히 어긋난다. 프리픽스로 나누면 호스트가 하나뿐이라 그럴 일이 없다.
 */
@Service
@RequiredArgsConstructor
@Slf4j(topic = "S3ImageService")
public class S3ImageService implements ImageService {

    private static final String ORIGIN_TYPE = "origin";
    private static final String DETAIL_TYPE = "detail";

    private static final String ORIGIN_PREFIX = ORIGIN_TYPE + "/";
    private static final String DETAIL_PREFIX = DETAIL_TYPE + "/";

    private final AmazonS3 s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /** 버킷은 OAC 로만 열려 있어 직접 접근이 막혀 있다. 저장하는 URL 은 항상 이 도메인을 쓴다. */
    @Value("${aws.cdn.base-url}")
    private String cdnBaseUrl;

    @Override
    public String uploadImage(String type, MultipartFile file) throws IOException {
        String prefix = prefixFor(type);
        if (prefix == null) {
            return null;
        }

        String key = prefix + generateFileName(Objects.requireNonNull(file.getOriginalFilename()));
        log.info("key: {}", key);
        s3Client.putObject(bucketName, key, file.getInputStream(), buildMetadata(file));
        return toCdnUrl(key);
    }

    @Override
    public String generateFileName(String originName) {
        String extension = originName.substring(originName.lastIndexOf("."));
        return UUID.randomUUID() + extension;
    }

    @Override
    public void deleteImage(String imgUrl) {
        s3Client.deleteObject(bucketName, extractKey(imgUrl));
    }

    private String prefixFor(String type) {
        if (ORIGIN_TYPE.equals(type)) {
            return ORIGIN_PREFIX;
        }
        if (DETAIL_TYPE.equals(type)) {
            return DETAIL_PREFIX;
        }
        return null;
    }

    private String toCdnUrl(String key) {
        return cdnBaseUrl.replaceAll("/+$", "") + "/" + key;
    }

    /**
     * 조회 URL 에서 S3 키를 되찾는다. 버킷이 하나뿐이라 호스트는 볼 필요가 없고, 경로가 곧 키다.
     * CloudFront 도메인이든 S3 도메인이든 같은 방식으로 동작한다.
     */
    private String extractKey(String imgUrl) {
        int schemeEnd = imgUrl.indexOf("://");
        String withoutScheme = schemeEnd < 0 ? imgUrl : imgUrl.substring(schemeEnd + 3);
        int pathStart = withoutScheme.indexOf('/');
        return pathStart < 0 ? withoutScheme : withoutScheme.substring(pathStart + 1);
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
}
