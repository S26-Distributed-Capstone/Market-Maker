package edu.yu.marketmaker.exchange;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import edu.yu.marketmaker.model.Quote;

@SpringBootTest
@AutoConfigureRestTestClient
public class QuoteTests {
    
    @Autowired
    private RestTestClient testClient;

    @Test
    void couldGetBackQuote() {
        Quote quote = new Quote("TEST", 1, 1, 1, 1, null, 0);
        testClient.put().uri("/quotes/TEST").body(quote).exchange();
        testClient.get().uri("/quotes/TEST").exchange().expectBody(Quote.class).isEqualTo(quote);
    }
}
