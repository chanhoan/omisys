package com.omisys.product.application.product;

import com.omisys.product.application.preorder.DistributedLockComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
// 트랜잭션은 ProductService 메서드가 락 안에서 시작·커밋한다. 여기서 트랜잭션을 열면 락 해제 후에 커밋되어 lost update가 생긴다
public class ProductLockService {

    private final DistributedLockComponent lockComponent;
    private final ProductService productService;

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
