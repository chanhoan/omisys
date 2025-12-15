package com.omisys.product.presentation.controller;

import com.omisys.product.application.product.ProductLockService;
import com.omisys.product.application.product.ProductService;
import com.omisys.product.product_dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class ProductInternalController {

    private final ProductService productService;
    private final ProductLockService productLockService;

    @GetMapping
    public List<ProductDto> getProductList(
            @RequestParam(name = "productIds") List<String> productIds) {
        return productService.getProductList(productIds);
    }

    @PostMapping("/reduce-stock")
    public void updateStock(@RequestBody Map<String, Integer> productQuantities) {
        productLockService.reduceStock(productQuantities);
    }

    @PostMapping("/rollback-stock")
    public void rollbackStock(@RequestBody Map<String, Integer> productQuantities) {
        productLockService.rollbackStock(productQuantities);
    }

}
