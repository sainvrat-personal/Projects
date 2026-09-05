package com.swifteats.order.payment;

import com.swifteats.common.domain.OrderStatus;
import com.swifteats.order.dto.PaymentProcessMessage;
import com.swifteats.order.dto.PaymentRequest;
import com.swifteats.order.dto.PaymentResult;
import com.swifteats.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessingService.class);

    private final MockPaymentGateway paymentGateway;
    private final OrderService orderService;

    public PaymentProcessingService(MockPaymentGateway paymentGateway, OrderService orderService) {
        this.paymentGateway = paymentGateway;
        this.orderService = orderService;
    }

    public void process(PaymentProcessMessage message) {
        log.info("Processing payment for order {}", message.orderId());
        PaymentResult result = paymentGateway.charge(new PaymentRequest(message.orderId(), message.amount()));
        if (result.success()) {
            orderService.transition(message.orderId(), OrderStatus.CONFIRMED, "PAYMENT_WORKER", null);
            log.info("Payment succeeded for order {}", message.orderId());
        } else {
            orderService.transition(
                    message.orderId(),
                    OrderStatus.PAYMENT_FAILED,
                    "PAYMENT_WORKER",
                    result.message());
            log.warn("Payment failed for order {}: {}", message.orderId(), result.message());
        }
    }
}
