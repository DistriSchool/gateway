package edu.unifor.br.distrischool.gateway.controller;

import edu.unifor.br.distrischool.gateway.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/auth/**")
    @PostMapping("/auth/**")

    @PutMapping("/auth/**")
    @DeleteMapping("/auth/**")
    @PatchMapping("/auth/**")
    public Mono<ResponseEntity<ErrorResponse>> authServiceFallback(ServerHttpRequest request) {
        log.warn("Circuit Breaker ATIVO para Auth Service | Rota: {} {} | Timestamp: {}",
                request.getMethod(),
                request.getURI().getPath(),
                LocalDateTime.now());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("O serviço de autenticação está temporariamente indisponível. Tente novamente em alguns instantes.")
                .build();

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse));
    }

    @GetMapping("/students/**")
    @PostMapping("/students/**")
    @PutMapping("/students/**")
    @DeleteMapping("/students/**")
    @PatchMapping("/students/**")
    public Mono<ResponseEntity<ErrorResponse>> studentServiceFallback(ServerHttpRequest request) {
        log.warn("Circuit Breaker ATIVO para Student Service | Rota: {} {} | Timestamp: {}",
                request.getMethod(),
                request.getURI().getPath(),
                LocalDateTime.now());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("O serviço de estudantes está temporariamente indisponível. Tente novamente em alguns instantes.")
                .build();

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse));
    }
}