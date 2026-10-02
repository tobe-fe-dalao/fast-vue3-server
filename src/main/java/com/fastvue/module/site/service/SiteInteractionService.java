package com.fastvue.module.site.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.common.response.PageRequest;
import com.fastvue.common.response.PageResponse;
import com.fastvue.common.response.QueryLimits;
import com.fastvue.module.site.api.BlogCommentVO;
import com.fastvue.module.site.api.CreateBlogCommentRequest;
import com.fastvue.module.site.api.CreateCheckoutRequest;
import com.fastvue.module.site.api.PaymentOrderVO;
import com.fastvue.module.site.persistence.BlogCommentEntity;
import com.fastvue.module.site.persistence.BlogCommentMapper;
import com.fastvue.module.site.persistence.PaymentOrderEntity;
import com.fastvue.module.site.persistence.PaymentOrderMapper;
import com.fastvue.security.LoginUser;
import com.fastvue.security.SecurityUtils;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for authenticated interactions on otherwise public site pages. */
@Service
@RequiredArgsConstructor
public class SiteInteractionService {

    private static final Map<Long, Plan> PAYABLE_PLANS =
            Map.of(
                    2L, new Plan("团队版", 29900),
                    3L, new Plan("企业版", 99900));

    private final BlogCommentMapper blogCommentMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final org.springframework.core.env.Environment environment;

    public List<BlogCommentVO> comments(Long articleId) {
        return QueryLimits.requireBounded(
                        blogCommentMapper.selectList(
                                commentQuery(articleId).last(QueryLimits.LEGACY_SQL_LIMIT)))
                .stream()
                .map(this::toCommentVO)
                .toList();
    }

    public PageResponse<BlogCommentVO> commentsPage(Long articleId, PageRequest request) {
        return PageResponse.map(
                blogCommentMapper.selectPage(
                        new Page<>(request.page(), request.pageSize()), commentQuery(articleId)),
                this::toCommentVO);
    }

    private LambdaQueryWrapper<BlogCommentEntity> commentQuery(Long articleId) {
        return new LambdaQueryWrapper<BlogCommentEntity>()
                .eq(BlogCommentEntity::getArticleId, articleId)
                .orderByDesc(BlogCommentEntity::getCreatedAt)
                .orderByDesc(BlogCommentEntity::getId);
    }

    @Transactional
    public BlogCommentVO createComment(Long articleId, CreateBlogCommentRequest request) {
        LoginUser user = SecurityUtils.currentUser();
        BlogCommentEntity entity = new BlogCommentEntity();
        entity.setTenantId(user.tenantId());
        entity.setArticleId(articleId);
        entity.setUserId(user.id());
        entity.setUsername(user.username());
        entity.setContent(request.content().trim());
        blogCommentMapper.insert(entity);
        return toCommentVO(entity);
    }

    @Transactional
    public PaymentOrderVO checkout(CreateCheckoutRequest request) {
        if (!environment.getProperty("app.demo.enabled", Boolean.class, false)) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "支付服务尚未配置");
        }
        LoginUser user = SecurityUtils.currentUser();
        Plan plan = PAYABLE_PLANS.get(request.planId());
        if (plan == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "该套餐无需在线支付或需要联系销售");
        }

        OffsetDateTime now = OffsetDateTime.now();
        PaymentOrderEntity entity = new PaymentOrderEntity();
        entity.setTenantId(user.tenantId());
        entity.setOrderNo(
                "FV"
                        + UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                                .substring(0, 24)
                                .toUpperCase(Locale.ROOT));
        entity.setUserId(user.id());
        entity.setPlanId(request.planId());
        entity.setPlanName(plan.name());
        entity.setAmountCents(plan.amountCents());
        entity.setCurrency("CNY");
        entity.setChannel(request.channel());
        entity.setStatus("pending");
        entity.setExpiresAt(now.plusMinutes(15));
        paymentOrderMapper.insert(entity);
        return toPaymentVO(entity);
    }

    private BlogCommentVO toCommentVO(BlogCommentEntity entity) {
        return new BlogCommentVO(
                entity.getId(),
                entity.getArticleId(),
                entity.getUsername(),
                entity.getContent(),
                entity.getCreatedAt());
    }

    private PaymentOrderVO toPaymentVO(PaymentOrderEntity entity) {
        return new PaymentOrderVO(
                entity.getId(),
                entity.getOrderNo(),
                entity.getPlanId(),
                entity.getPlanName(),
                entity.getAmountCents(),
                entity.getCurrency(),
                entity.getChannel(),
                entity.getStatus(),
                "/payment/demo/" + entity.getOrderNo(),
                entity.getCreatedAt(),
                entity.getExpiresAt());
    }

    private record Plan(String name, int amountCents) {}
}
