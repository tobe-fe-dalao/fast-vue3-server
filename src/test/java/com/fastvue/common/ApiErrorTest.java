package com.fastvue.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fastvue.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

class ApiErrorTest {
    @RestController
    static class Endpoint {
        @PostMapping("/error-test/{id}")
        String write(
                @PathVariable Long id,
                @RequestHeader("Idempotency-Key") String key,
                @RequestBody java.util.Map<String, String> body) {
            return "ok";
        }

        @PostMapping("/upload-test")
        String upload(@RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
            return "ok";
        }
    }

    @Test
    void badInputsAreClientErrors() throws Exception {
        var mvc =
                MockMvcBuilders.standaloneSetup(new Endpoint())
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
        mvc.perform(post("/error-test/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/error-test/1")).andExpect(status().isBadRequest());
        mvc.perform(
                        post("/error-test/1")
                                .header("Idempotency-Key", "key")
                                .contentType("application/json")
                                .content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/upload-test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(get("/error-test/1")).andExpect(status().isMethodNotAllowed());
    }
}
