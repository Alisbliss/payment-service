package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.dto.PaymentCreateRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.exception.InvalidTokenException;
import com.innowise.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @paymentService.isOrderOwner(#request.orderId, authentication.principal.claims['user_id'])")
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody PaymentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @paymentService.isPaymentOwner(#id, authentication.principal.claims['user_id'])")
    public ResponseEntity<PaymentResponse> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PaymentResponse>> getMy(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Status status,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(paymentService.getAll(userId, null, status, pageable));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> getAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Status status,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(paymentService.getAll(userId, orderId, status, pageable));
    }

    @GetMapping("/total")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BigDecimal> getTotalForCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(paymentService.getTotalForCurrentUser(userId, from, to));
    }

    @GetMapping("/total/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BigDecimal> getTotalForAllUsers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(paymentService.getTotalForAllUsers(from, to));
    }

    private Long extractUserId(Jwt jwt) {
        String userId = jwt.getClaimAsString("user_id");
        if (userId == null) {
            throw new InvalidTokenException("Token does not contain user_id claim");
        }
        return Long.valueOf(userId);
    }
}