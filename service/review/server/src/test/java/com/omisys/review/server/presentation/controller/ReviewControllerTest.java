package com.omisys.review.server.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.review.server.application.service.ReviewService;
import com.omisys.review.server.infrastructure.filter.JwtAuthentication;
import com.omisys.review.server.presentation.request.ReviewRequest;
import com.omisys.review.server.presentation.response.ReviewResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;
    private static final Long REVIEW_ID = 10L;
    private static final String PRODUCT_ID = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver()
                )
                .build();

        JwtClaim claim = JwtClaim.create(USER_ID, "testUser", "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(JwtAuthentication.create(claim));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("createReview: HTTP 200 반환 (ResponseEntity 미사용 컨벤션)")
    void createReview_returns_200() throws Exception {
        when(reviewService.createReview(anyLong(), any())).thenReturn(REVIEW_ID);

        ReviewRequest.Create request = ReviewRequest.Create.builder()
                .productId(PRODUCT_ID)
                .orderId(100L)
                .rating(5)
                .content("좋아요")
                .build();

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusName").value("CREATED"))
                .andExpect(jsonPath("$.data").value(REVIEW_ID.intValue()));
    }

    @Test
    @DisplayName("getReviews: HTTP 200 반환")
    void getReviews_returns_200() throws Exception {
        Page<ReviewResponse.Summary> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(reviewService.getReviews(anyString(), any())).thenReturn(page);

        mockMvc.perform(get("/api/reviews")
                        .param("productId", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusName").value("OK"));
    }

    @Test
    @DisplayName("updateReview: HTTP 200 반환")
    void updateReview_returns_200() throws Exception {
        doNothing().when(reviewService).updateReview(anyLong(), anyLong(), any());

        ReviewRequest.Update request = ReviewRequest.Update.builder()
                .rating(4)
                .content("수정 내용")
                .build();

        mockMvc.perform(patch("/api/reviews/{reviewId}", REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusName").value("OK"));
    }

    @Test
    @DisplayName("deleteReview: HTTP 200 반환")
    void deleteReview_returns_200() throws Exception {
        doNothing().when(reviewService).deleteReview(anyLong(), anyLong());

        mockMvc.perform(delete("/api/reviews/{reviewId}", REVIEW_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusName").value("OK"));
    }
}
