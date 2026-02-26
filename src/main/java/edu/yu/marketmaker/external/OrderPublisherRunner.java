package edu.yu.marketmaker.external;

import edu.yu.marketmaker.external.ExternalOrderPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("external-publisher")
public class OrderPublisherRunner implements ApplicationRunner {

    @Value("${exchange.base-url}")
    private String exchangeBaseUrl;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ExternalOrderPublisher publisher = new ExternalOrderPublisher(exchangeBaseUrl);
        publisher.startGeneratingOrders();
    }
}