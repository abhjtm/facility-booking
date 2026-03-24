package io.github.abhjtm.booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "booking.storage.type=file",
        "booking.storage.file=target/test-data/bookings.jsonl",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class ApplicationTests {

    @Test
    void contextLoads() {
    }
}
