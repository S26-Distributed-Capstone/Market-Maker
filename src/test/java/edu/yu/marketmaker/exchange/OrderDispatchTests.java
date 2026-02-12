package edu.yu.marketmaker.exchange;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import edu.yu.marketmaker.model.ExternalOrder;
import edu.yu.marketmaker.model.Side;

@SpringBootTest
@AutoConfigureRestTestClient
public class OrderDispatchTests {
    
    @Autowired
    private RestTestClient testClient;

    @Test
    void failsOnInvalidOrders() {
        testClient.post().uri("/orders").body(new ExternalOrder("AAA", 100, -1, Side.BUY))
            .exchange().expectStatus().is4xxClientError();
        testClient.post().uri("/orders").body(new ExternalOrder("AAA", -1, 100, Side.SELL))
            .exchange().expectStatus().is4xxClientError();
    }

    @Test
    void succeedsOnValidOrders() {
        testClient.post().uri("/orders").body(new ExternalOrder("AAA", 100, 10, Side.BUY))
            .exchange().expectStatus().is2xxSuccessful();
    }
}
