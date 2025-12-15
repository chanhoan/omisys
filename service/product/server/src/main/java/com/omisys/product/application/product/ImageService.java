package com.omisys.product.application.product;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageService {

    String uploadImage(String type, MultipartFile file) throws IOException;

    String generateFileName(String originName);

    void deleteImage(String imgUrl);

}
