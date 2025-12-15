package com.omisys.product.application.preorder;

import com.omisys.product.domain.model.PreOrder;
import com.omisys.product.domain.model.PreOrderState;
import com.omisys.product.domain.model.Product;
import com.omisys.product.domain.repository.cassandra.ProductRepository;
import com.omisys.product.domain.repository.jpa.PreOrderRepository;
import com.omisys.product.exception.ProductErrorCode;
import com.omisys.product.exception.ProductException;
import com.omisys.product.presentation.request.PreOrderRequest;
import com.omisys.product.presentation.response.PreOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PreOrderService {

    private final PreOrderRepository preOrderRepository;
    private final ProductRepository productRepository;

    public void createPreOrder(PreOrderRequest.Create request) {
        Product product = getProductByProductId(request.getProductId());
        validateStock(product.getStock(), request.getAvailableQuantity());
        PreOrder preOrder = PreOrder.create(request);
        preOrderRepository.save(preOrder);
    }

    @CacheEvict(cacheNames = "preOrder", key = "#request.preOrderId()")
    public void updatePreOrder(PreOrderRequest.Update request) {
        PreOrder preOrder = getPreOrderByPreOrderId(request.getPreOrderId());
        if (!Objects.equals(preOrder.getPreOrderId(), request.getPreOrderId())) {
            Product product = getProductByProductId(request.getProductId());
            validateStock(product.getStock(), request.getAvailableQuantity());
        }
        preOrder.update(request);
    }

    @Transactional(readOnly = true)
    public PreOrderResponse getPreOrder(long preOrderId) {
        return PreOrderResponse.of(getPreOrderByPreOrderId(preOrderId));
    }

    @Transactional(readOnly = true)
    public Page<PreOrderResponse> getPreOrderList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return preOrderRepository.findAllByIsPublicTrue(pageable).map(PreOrderResponse::of);
    }

    @CacheEvict(cacheNames = "preOrder", key = "#preOrderId")
    public PreOrderResponse updateState(Long preOrderId, PreOrderState state) {
        PreOrder preOrder = getPreOrderByPreOrderId(preOrderId);
        if (state == PreOrderState.OPEN_FOR_ORDER) preOrder.open();
        else if (state == PreOrderState.CANCELED) preOrder.cancel();
        return PreOrderResponse.of(preOrder);
    }

    @CacheEvict(cacheNames = "preOrder", key = "#preOrderId")
    public void deletePreOrder(long preOrderId) {
        PreOrder preOrder = getPreOrderByPreOrderId(preOrderId);
        preOrderRepository.delete(preOrder);
    }

    private void validateStock(int nowQuantity, int requestQuantity) {
        if (nowQuantity <= requestQuantity) {
            throw new ProductException(ProductErrorCode.PREORDER_QUANTITY_CONFLICT);
        }
    }

    private PreOrder getPreOrderByPreOrderId(long preOrderId) {
        return preOrderRepository
                .findByPreOrderIdAndIsPublicTrue(preOrderId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.NOT_FOUND_PREORDER));
    }

    private Product getProductByProductId(UUID productId) {
        return productRepository
                .findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.NOT_FOUND_PRODUCT));
    }
}
