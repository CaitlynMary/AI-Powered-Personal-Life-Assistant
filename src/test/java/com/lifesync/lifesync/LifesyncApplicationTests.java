package com.lifesync.lifesync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "app.jwt.secret=VGhpcy1pcy1vbmx5LWEtc2FmZS10ZXN0LXNpZ25pbmcta2V5LWZvci1saWZlc3luYw==")
@ActiveProfiles("test")
class LifesyncApplicationTests {

	@Test
	void contextLoads() {
	}

}
