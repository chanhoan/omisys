package com.omisys.common.domain.entity;

public class KafkaTopicConstant {

    public static final String PROCESS_PREORDER = "process_preorder";
    public static final String ERROR_IN_PROCESS_PREORDER = "error_in_create_delivery";
    public static final String PAYMENT_COMPLETED = "payment-completed-topic";
    public static final String PROVIDE_EVENT_COUPON = "provide-event-coupon";

    // Dead Letter Topics — 재시도 소진 후 메시지가 라우팅되는 토픽
    // DeadLetterPublishingRecoverer가 원본토픽 + ".DLT" 규칙으로 자동 라우팅하므로
    // 이 상수들은 DLT Consumer 또는 모니터링 구성 시 참조값으로 사용한다.
    public static final String PROCESS_PREORDER_DLT = PROCESS_PREORDER + ".DLT";
    public static final String PROVIDE_EVENT_COUPON_DLT = PROVIDE_EVENT_COUPON + ".DLT";

}
