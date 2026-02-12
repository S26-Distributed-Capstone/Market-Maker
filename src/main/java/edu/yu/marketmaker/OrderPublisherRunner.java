package edu.yu.marketmaker;

import edu.yu.marketmaker.external.ExternalOrderPublisher;

public class OrderPublisherRunner {
    public static void main(String[] args) {
        String exchangeBaseUrl = "http://localhost:8080/orders"; // Adjust as needed
        try {
            ExternalOrderPublisher publisher = new ExternalOrderPublisher(exchangeBaseUrl);
            publisher.startGeneratingOrders();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
