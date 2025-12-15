package com.omisys.product.application.preorder;

import com.omisys.product.domain.repository.redis.RedisRepository;
import com.omisys.product.exception.ProductErrorCode;
import com.omisys.product.exception.ProductException;
import com.omisys.product.infrastructure.utils.PreOrderRedisDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.omisys.product.infrastructure.utils.RedisUtils.getRedisKeyOfPreOrder;

@Service
@RequiredArgsConstructor
public class PreOrderRedisService {

    private final RedisRepository redisRepository;

    public void validateQuantity(PreOrderRedisDto cache, long userId) {
        if (!availableUser(cache.preOrderId(), userId)) {
            throw new ProductException(ProductErrorCode.ALREADY_PREORDER);
        }
        if (!availableQuantity(cache.availableQuantity(), cache.preOrderId())) {
            throw new ProductException(ProductErrorCode.EXCEED_PREORDER_QUANTITY);
        }
    }

    public void preOrder(String key, long userId) {
        redisRepository.sAdd(key, Long.toString(userId));
    }

    private boolean availableUser(long preOrderId, long userId) {
        String key = getRedisKeyOfPreOrder(preOrderId);
        return !redisRepository.sIsMember(key, Long.toString(userId));
    }

    private boolean availableQuantity(int availableQuantity, long preOrderId) {
        String key = getRedisKeyOfPreOrder(availableQuantity);
        return availableQuantity > redisRepository.sCard(key);
    }

}
