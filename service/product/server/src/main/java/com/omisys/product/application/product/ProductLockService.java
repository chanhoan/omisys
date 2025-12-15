package com.omisys.product.application.product;

import com.omisys.product.application.preorder.DistributedLockComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductLockService {

    private final DistributedLockComponent lockComponent;
    private final ProductService productService;

    @Transactional
    public void reduceStock(Map<String, Integer> productQuantities) {

        Set<String> productIds = productQuantities.keySet();
        lockComponent.executeForMultipleProducts(
                productIds.stream().map("stockLock_%s"::formatted).toList(),
                3000,
                3000,
                3,
                3000,
                () -> {
                    productService.reduceStock(productQuantities);
                });
    }

    @Transactional
    public void rollbackStock(Map<String, Integer> productQuantities) {
        Set<String> productIds = productQuantities.keySet();
        lockComponent.executeForMultipleProducts(
                productIds.stream().map("stockLock_%s"::formatted).toList(),
                3000,
                3000,
                3,
                3000,
                () -> {
                    productService.rollbackStock(productQuantities);
                });
    }

}
