package com.example.gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerFilterFactory;
import org.springframework.stereotype.Component;

//Данный кастомный фильтр позволяет задать порядок фильтров(сначала идёт СircuitBreaker, затем все остальные фильтры)
//Добавлен для корректной работы fallbackUri
@Component
public class CircuitBreakerLoadBalancerFilter extends AbstractGatewayFilterFactory<Object> {

    private final SpringCloudCircuitBreakerFilterFactory circuitBreakerFactory;

    public CircuitBreakerLoadBalancerFilter(SpringCloudCircuitBreakerFilterFactory circuitBreakerFactory) {
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> circuitBreakerFactory
                .apply(circuitBreakerFactory.newConfig()
                        .setName("taskManagerCircuitBreaker")
                        .setFallbackUri("forward:/fallback/task-manager"))
                .filter(exchange, chain);
    }
}
