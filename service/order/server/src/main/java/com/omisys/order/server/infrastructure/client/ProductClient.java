package com.omisys.order.server.infrastructure.client;

import com.omisys.product.product_dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "product")
public interface ProductClient {

    @GetMapping("/internal/products")
    List<ProductDto> getProductList(@RequestParam(name = "productIds") List<String> productIds);

    @PostMapping("/internal/products/reduce-stock")
    void updateStock(@RequestBody Map<String, Integer> productQuantities);

    @PostMapping("/internal/products/rollback-stock")
    void rollbackStock(@RequestBody Map<String, Integer> productQuantities);

}