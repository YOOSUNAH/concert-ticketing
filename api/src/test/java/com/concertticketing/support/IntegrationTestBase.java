package com.concertticketing.support;

import com.concertticketing.domain.auth.dto.LoginRequest;
import com.concertticketing.domain.user.dto.SignUpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("concert")
            .withUsername("concert")
            .withPassword("concert");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        // 이 통합테스트들은 알림(Kafka) 흐름을 검증하지 않는다. 브로커 없이도 돌도록
        // 컨슈머는 띄우지 않고, 결제 시 발행되는 produce는 빠르게 실패(fast-fail)시킨다.
        // Kafka 발행→소비 검증은 @EmbeddedKafka 슬라이스 테스트가 따로 담당한다.
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("spring.kafka.producer.properties.max.block.ms", () -> "500");
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestDataReset testDataReset;

    protected WebTestClient webTestClient;

    @BeforeEach
    void setUpBase() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        testDataReset.cleanTransactional();
    }

    /**
     * 회원가입 → 로그인 → JWT 토큰 추출
     */
    protected String signUpAndLogin(String email, String password, String name) {
        webTestClient.post().uri("/users")
                .bodyValue(new SignUpRequest(email, password, name))
                .exchange()
                .expectStatus().isCreated();

        return login(email, password);
    }

    protected String login(String email, String password) {
        String authHeader = webTestClient.post().uri("/auth/login")
                .bodyValue(new LoginRequest(email, password))
                .exchange()
                .expectStatus().isOk()
                .returnResult(Void.class)
                .getResponseHeaders()
                .getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("Authorization 헤더가 없습니다.");
        }
        return authHeader.substring("Bearer ".length());
    }
}
