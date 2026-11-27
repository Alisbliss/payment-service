package com.innowise.paymentservice;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.paymentservice.dto.PaymentCreateRequest;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.event.PaymentEventProducer;
import com.innowise.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PaymentFlowIntegrationTest {

    static WireMockServer wireMock;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private PaymentEventProducer paymentEventProducer;

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> "mongodb://" + mongo.getHost() + ":"
                + mongo.getMappedPort(27017) + "/paymentdb?directConnection=true");
        registry.add("order-service.url", () -> "http://localhost:" + wireMock.port());
        registry.add("random-api.url", () -> "http://localhost:" + wireMock.port());
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:" + wireMock.port() + "/.well-known/jwks.json");
    }

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(0);
        wireMock.start();
        WireMock.configureFor("localhost", wireMock.port());
    }

    @AfterAll
    static void stopWireMock() { wireMock.stop(); }

    @AfterEach
    void resetWireMock() { wireMock.resetAll(); }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PaymentRepository paymentRepository;

    private String userToken;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();

        when(jwtDecoder.decode(anyString())).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            String[] parts = token.split("-");
            if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid test token: " + token);
            }
            Long userId = Long.parseLong(parts[1]);
            String role = parts[2];
            String type = parts.length > 3 ? parts[3] : "access";
            return Jwt.withTokenValue(token)
                    .header("alg", "RS256")
                    .claim("userId", userId)
                    .claim("role", role)
                    .claim("type", type)
                    .build();
        });
        this.userToken = "token-5-USER";

        //Order Service stub-order belongs to user 5
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/api/orders/.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {"id":1,"userId":5,"totalPrice":100.00}
                        """)));

        //Random API stub-even number-SUCCESS
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/random"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("42")));
    }

    private String generateToken(Long userId, String role) {
        return "token-" + userId + "-" + role;
    }

    private PaymentCreateRequest createRequest() {
        return PaymentCreateRequest.builder().orderId(1L).build();
    }

    //CREATE
    @Test
    void create_shouldReturn201WithSuccessStatusWhenRandomIsEven() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.paymentAmount").value(100.00))
                .andExpect(jsonPath("$.order.id").value(1));

        assertThat(paymentRepository.findAll()).hasSize(1);
    }

    @Test
    void create_shouldReturn201WithFailedStatus_whenRandomIsOdd() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/random"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("7")));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void create_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_shouldReturn400WhenOrderIdIsNull() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn403WhenOrderBelongsToAnotherUser() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/api/orders/.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {"id":1,"userId":99,"totalPrice":100.00}
                        """)));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_shouldReturn503WhenOrderServiceUnavailable() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/api/orders/.*"))
                .willReturn(WireMock.aResponse().withStatus(500)));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isServiceUnavailable());
    }

    //GET by id
    @Test
    void getById_shouldReturn200ForOwner() throws Exception {
        Payment saved = paymentRepository.save(Payment.builder()
                .orderId(1L).userId(5L).status(Status.SUCCESS)
                .paymentAmount(new BigDecimal("100.00"))
                .timestamp(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/payments/{id}", saved.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.userId").value(5));
    }

    @Test
    void getById_shouldReturn403ForAnotherUser() throws Exception {
        Payment saved = paymentRepository.save(Payment.builder()
                .orderId(1L).userId(99L).status(Status.SUCCESS)
                .paymentAmount(new BigDecimal("100.00"))
                .timestamp(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/payments/{id}", saved.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/payments/{id}", 999)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    //GET /my
    @Test
    void getMy_shouldReturnOnlyCurrentUserPayments() throws Exception {
        paymentRepository.save(Payment.builder()
                .orderId(1L).userId(5L).status(Status.SUCCESS)
                .paymentAmount(new BigDecimal("100.00")).timestamp(LocalDateTime.now()).build());
        paymentRepository.save(Payment.builder()
                .orderId(2L).userId(99L).status(Status.FAILED)
                .paymentAmount(new BigDecimal("50.00")).timestamp(LocalDateTime.now()).build());

        mockMvc.perform(get("/api/payments/my")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(5));
    }

    //GET (admin)
    @Test
    void getAll_shouldReturn403ForUser() throws Exception {
        mockMvc.perform(get("/api/payments")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_shouldReturn200ForAdmin() throws Exception {
        String adminToken = generateToken(1L, "ADMIN");

        mockMvc.perform(get("/api/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAll_shouldFilterByUserIdAndStatus() throws Exception {
        String adminToken = generateToken(1L, "ADMIN");
        paymentRepository.save(Payment.builder()
                .orderId(1L).userId(5L).status(Status.SUCCESS)
                .paymentAmount(new BigDecimal("100.00")).timestamp(LocalDateTime.now()).build());
        paymentRepository.save(Payment.builder()
                .orderId(2L).userId(5L).status(Status.FAILED)
                .paymentAmount(new BigDecimal("50.00")).timestamp(LocalDateTime.now()).build());
        paymentRepository.save(Payment.builder()
                .orderId(3L).userId(99L).status(Status.SUCCESS)
                .paymentAmount(new BigDecimal("25.00")).timestamp(LocalDateTime.now()).build());

        mockMvc.perform(get("/api/payments")
                        .param("userId", "5")
                        .param("status", "SUCCESS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].orderId").value(1));
    }

    //total
    @Test
    void getTotalForCurrentUser_shouldSumOnlyCurrentUser() throws Exception {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);

        paymentRepository.save(Payment.builder()
                .orderId(1L).userId(5L).status(Status.SUCCESS)
                .paymentAmount(new BigDecimal("100.00")).timestamp(LocalDateTime.now()).build());
        paymentRepository.save(Payment.builder()
                .orderId(2L).userId(5L).status(Status.FAILED)
                .paymentAmount(new BigDecimal("50.00")).timestamp(LocalDateTime.now()).build());
        paymentRepository.save(Payment.builder()
                .orderId(3L).userId(99L).status(Status.SUCCESS)
                .paymentAmount(new BigDecimal("25.00")).timestamp(LocalDateTime.now()).build());

        mockMvc.perform(get("/api/payments/total")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(150.00));
    }

    @Test
    void getTotalForAllUsers_shouldReturn403ForUser() throws Exception {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);

        mockMvc.perform(get("/api/payments/total/all")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTotalForAllUsers_shouldReturn200ForAdmin() throws Exception {
        String adminToken = generateToken(1L, "ADMIN");
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);

        paymentRepository.save(Payment.builder()
                .orderId(1L).userId(5L).status(Status.SUCCESS)
                .paymentAmount(new BigDecimal("100.00")).timestamp(LocalDateTime.now()).build());
        paymentRepository.save(Payment.builder()
                .orderId(2L).userId(99L).status(Status.SUCCESS)
                .paymentAmount(new BigDecimal("25.00")).timestamp(LocalDateTime.now()).build());

        mockMvc.perform(get("/api/payments/total/all")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(125.00));
    }
}
