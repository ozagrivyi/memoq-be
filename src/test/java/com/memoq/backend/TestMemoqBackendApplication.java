package com.memoq.backend;

import org.springframework.boot.SpringApplication;

public class TestMemoqBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(MemoqBackendApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
