package com.omisys.delivery.server.application.service.mapper;

import com.omisys.delivery.server.domain.model.Delivery;
import com.omisys.delivery.server.domain.model.DeliveryTrackingHistory;
import com.omisys.delivery.server.presentation.response.DeliveryResponse;

public class DeliveryMapper {

    private DeliveryMapper() {}

    public static DeliveryResponse.Get toGetResponse(Delivery d) {
        return new DeliveryResponse.Get(
                d.getDeliveryId(),
                d.getOrderId(),
                d.getUserId(),
                d.getState().name(),
                d.getCourier() != null ? d.getCourier().name() : null,
                d.getInvoiceNumber(),
                d.getRecipient(),
                d.getPhoneNumber(),
                d.getZipcode(),
                d.getShippingAddress()
        );
    }

    public static DeliveryResponse.MyGet toMyGetResponse(Delivery d) {
        return new DeliveryResponse.MyGet(
                d.getDeliveryId(),
                d.getOrderId(),
                d.getState().name(),
                d.getCourier() != null ? d.getCourier().name() : null,
                d.getInvoiceNumber()
        );
    }

    public static DeliveryResponse.AdminGet toAdminGetResponse(Delivery d) {
        return new DeliveryResponse.AdminGet(
                d.getDeliveryId(),
                d.getOrderId(),
                d.getUserId(),
                d.getState().name(),
                d.getCourier() != null ? d.getCourier().name() : null,
                d.getInvoiceNumber(),
                d.getCreatedAt()
        );
    }

    public static DeliveryResponse.TrackingHistory toTrackingResponse(DeliveryTrackingHistory h) {
        return new DeliveryResponse.TrackingHistory(
                h.getState().name(),
                h.getMemo(),
                h.getOccurredAt()
        );
    }
}
