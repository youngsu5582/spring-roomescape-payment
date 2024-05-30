package roomescape.payment.service;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import roomescape.global.util.Encoder;
import roomescape.payment.PaymentProperties;
import roomescape.payment.service.dto.PaymentCancelRequest;
import roomescape.payment.service.dto.PaymentErrorResponse;
import roomescape.payment.service.dto.PaymentRequest;
import roomescape.payment.service.dto.PaymentResponse;
import roomescape.payment.exception.PaymentException;

@Component
public class PaymentClient {
    private static final String PREFIX = "Basic ";
    private static final String PAYMENT_CONFIRM_URI = "/v1/payments/confirm";
    private static final String PAYMENT_CANCEL_URI = "/v1/payments/%s/cancel";
    private final RestClient restClient;

    public PaymentClient(final Encoder encoder, final PaymentProperties paymentProperties, ClientHttpRequestFactory factory) {
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(paymentProperties.getHostName())
                .defaultHeader("Authorization", PREFIX + encoder.encode(paymentProperties.getSecretKey()))
                .build();
    }

    public PaymentResponse confirm(PaymentRequest paymentRequest) {
        try {
            return restClient.post()
                    .uri(PAYMENT_CONFIRM_URI)
                    .body(paymentRequest)
                    .retrieve()
                    .toEntity(PaymentResponse.class)
                    .getBody();
        } catch (HttpClientErrorException e) {
            PaymentErrorResponse paymentResponse = e.getResponseBodyAs(PaymentErrorResponse.class);
            throw new PaymentException(paymentResponse);
        }
    }

    public PaymentResponse cancel(String paymentKey) {
        try {
            return restClient.post()
                    .uri(String.format(PAYMENT_CANCEL_URI, paymentKey))
                    .body(new PaymentCancelRequest())
                    .retrieve()
                    .toEntity(PaymentResponse.class)
                    .getBody();
        } catch (HttpClientErrorException e) {
            PaymentErrorResponse paymentResponse = e.getResponseBodyAs(PaymentErrorResponse.class);
            throw new PaymentException(paymentResponse);
        }
    }
}
