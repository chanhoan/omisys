package com.omisys.order.server.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.order.order_dto.dto.NotificationOrderDto;
import com.omisys.order.server.application.service.mapper.OrderMapper;
import com.omisys.order.server.domain.model.Order;
import com.omisys.order.server.domain.model.OrderProduct;
import com.omisys.order.server.domain.model.vo.OrderState;
import com.omisys.order.server.domain.repository.OrderProductRepository;
import com.omisys.order.server.domain.repository.OrderRepository;
import com.omisys.order.server.exception.OrderErrorCode;
import com.omisys.order.server.exception.OrderException;
import com.omisys.order.server.infrastructure.client.PaymentClient;
import com.omisys.order.server.infrastructure.client.ProductClient;
import com.omisys.order.server.infrastructure.client.UserClient;
import com.omisys.order.server.presentation.response.OrderResponse;
import com.omisys.payment.payment_dto.dto.PaymentInternalDto;
import com.omisys.payment.payment_dto.dto.PaymentInternalDto.Cancel;
import com.omisys.user_dto.infrastructure.AddressDto;
import com.omisys.user_dto.infrastructure.PointHistoryDto;
import com.omisys.user_dto.infrastructure.UserDto;
import com.omisys.user_dto.infrastructure.UserDto.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j(topic = "OrderService")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final PaymentClient paymentClient;
    private final ProductClient productClient;
    private final OrderProductRepository orderProductRepository;
    private final OrderCreateService orderCreateService;
    private static final String POINT_HISTORY_TYPE_REFUND = "환불";
    private static final String POINT_DESCRIPTION_ORDER_CANCEL = "주문 취소";
    private static final String CANCEL_REASON = "단순 변심";

    @Transactional
    public Long cancelOrder(Long userId, Long orderId) {
        UserDto user = userClient.getUser(userId);
        Order order = validateOrderExists(orderId);
        order.validateOrderPermission(user);
        order.validateOrderUpdate();

        refundPoint(userId, orderId, order.getPointPrice());
        rollbackStock(order);

        if (order.getState().equals(OrderState.COMPLETED)) {
            cancelPayment(orderId);
        }

        // TODO 쿠폰 사용 롤백
        order.cancel();
        return orderId;
    }

    @Transactional
    public Long updateOrderState(Long userId, Long orderId, String orderState) {
        UserDto user = userClient.getUser(userId);

        if (user.getRole().equals(UserRole.ROLE_USER.name())) {
            throw new OrderException(OrderErrorCode.ORDER_PERMISSION_DENIED);
        }

        Order order = validateOrderExists(orderId);

        try {
            OrderState state = OrderState.valueOf(orderState);
            order.updateState(state);
        } catch (IllegalArgumentException e) {
            throw new OrderException(OrderErrorCode.ORDER_STATE_NOT_FOUND);
        }

        return orderId;
    }

    @Transactional
    public Long updateOrderAddress(Long userId, Long orderId, Long addressId) {
        UserDto user = userClient.getUser(userId);
        Order order = validateOrderExists(orderId);
        order.validateOrderPermission(user);
        order.validateOrderUpdate();

        AddressDto address = userClient.getAddress(addressId);
        orderCreateService.validateAddress(address, userId);

        order.updateAddress(address);
        return orderId;
    }

    @Transactional
    public Long registerOrderInvoiceNumber(Long userId, Long orderId, String invoiceNumber) {
        UserDto user = userClient.getUser(userId);
        Order order = validateOrderExists(orderId);
        order.validateOrderPermission(user);

        order.registerOrderInvoiceNumber(invoiceNumber);
        return orderId;
    }

    public OrderResponse.OrderGetResponse getOrder(Long userId, Long orderId) {
        UserDto user = userClient.getUser(userId);
        Order order = validateOrderExists(orderId);
        order.validateOrderPermission(user);

        final List<OrderResponse.OrderProductResponse> orderProductResponses = getOrderProductResponses(order);
        final PaymentInternalDto.Get payment = paymentClient.getPayment(orderId);

        return OrderResponse.OrderGetResponse.from(order, orderProductResponses, payment);
    }

    public Page<OrderResponse.MyOrderGetResponse> getMyOrder(Pageable pageable, Long userId, String keyword) {
        UserDto user = userClient.getUser(userId);
        Page<Order> orders = orderRepository.getMyOrder(pageable, userId, keyword);

        List<OrderResponse.MyOrderGetResponse> responses = new ArrayList<>();
        orders.forEach(
                order -> responses.add(OrderResponse.MyOrderGetResponse.from(order, getMyOrderProductResponses(order))));

        return new PageImpl<>(responses, pageable, orders.getTotalElements());
    }

    public Page<OrderResponse.AllOrderGetResponse> getAllOrder(Pageable pageable, Long userId, Long orderUserId,
                                                               String productId) {
        UserDto user = userClient.getUser(userId);
        Page<Order> orders = orderRepository.getAllOrder(pageable, orderUserId, productId);

        List<OrderResponse.AllOrderGetResponse> responses = new ArrayList<>();
        orders.forEach(
                order -> responses.add(OrderResponse.AllOrderGetResponse.from(order)));

        return new PageImpl<>(responses, pageable, orders.getTotalElements());
    }

    public NotificationOrderDto getNotificationOrder(Long orderId) {
        Order order = validateOrderExists(orderId);
        String displayProductName = order.getOrderProducts().get(0).getProductName();
        return OrderMapper.toNotificationOrderDto(order, displayProductName);
    }

    @Transactional
    public void deleteOrder(Long userId, Long orderId) {
        UserDto user = userClient.getUser(userId);
        Order order = validateOrderExists(orderId);
        order.validateOrderPermission(user);
        order.validateOrderDelete();
        order.delete();
    }


    public Order validateOrderExists(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(
                () -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND)
        );
    }

    private List<OrderResponse.OrderProductResponse> getOrderProductResponses(Order order) {
        final List<OrderProduct> orderProducts = order.getOrderProducts();
        return orderProducts.stream().map(OrderResponse.OrderProductResponse::fromEntity).toList();
    }

    private List<OrderResponse.MyOrderProductResponse> getMyOrderProductResponses(Order order) {
        final List<OrderProduct> orderProducts = order.getOrderProducts();
        return orderProducts.stream().map(OrderResponse.MyOrderProductResponse::fromEntity).toList();
    }

    private void refundPoint(Long userId, Long orderId, BigDecimal pointPrice) {
        PointHistoryDto pointHistory = new PointHistoryDto(userId, orderId, pointPrice
                , POINT_HISTORY_TYPE_REFUND, POINT_DESCRIPTION_ORDER_CANCEL);
        userClient.createPointHistory(pointHistory);
    }

    private void rollbackStock(Order order) {
        List<OrderProduct> orderProducts = orderProductRepository.findByOrder(order);
        Map<String, Integer> orderProductQuantities = orderProducts.stream().collect(Collectors.toMap(
                OrderProduct::getProductId, OrderProduct::getQuantity));

        productClient.rollbackStock(orderProductQuantities);
    }

    private void cancelPayment(Long orderId) {
        PaymentInternalDto.Cancel paymentCancel = new Cancel(orderId, CANCEL_REASON);
        paymentClient.cancelPayment(paymentCancel);
    }


}