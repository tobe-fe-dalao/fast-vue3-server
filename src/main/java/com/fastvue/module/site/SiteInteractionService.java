package com.fastvue.module.site;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fastvue.common.BusinessException;
import com.fastvue.common.ErrorCode;
import com.fastvue.security.LoginUser;
import com.fastvue.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Business logic for authenticated interactions on otherwise public site pages. */
@Service
@RequiredArgsConstructor
public class SiteInteractionService {

    private static final Map<Long, Plan> PAYABLE_PLANS = Map.of(
            2L, new Plan("团队版", 29900),
            3L, new Plan("企业版", 99900));

    private final BlogCommentMapper blogCommentMapper;
    private final PaymentOrderMapper paymentOrderMapper;

    public List<BlogCommentVO> comments(Long articleId) {
        return blogCommentMapper.selectList(new LambdaQueryWrapper<BlogCommentEntity>()
                        .eq(BlogCommentEntity::getArticleId, articleId)
                        .orderByDesc(BlogCommentEntity::getCreatedAt))
                .stream()
                .map(this::toCommentVO)
                .toList();
    }

    @Transactional
    public BlogCommentVO createComment(Long articleId, CreateBlogCommentRequest request) {
        LoginUser user = SecurityUtils.currentUser();
        BlogCommentEntity entity = new BlogCommentEntity();
        entity.setArticleId(articleId);
        entity.setUserId(user.id());
        entity.setUsername(user.username());
        entity.setContent(request.content().trim());
        blogCommentMapper.insert(entity);
        return toCommentVO(entity);
    }

    @Transactional
    public PaymentOrderVO checkout(CreateCheckoutRequest request) {
        LoginUser user = SecurityUtils.currentUser();
        Plan plan = PAYABLE_PLANS.get(request.planId());
        if (plan == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "该套餐无需在线支付或需要联系销售");
        }

        OffsetDateTime now = OffsetDateTime.now();
        PaymentOrderEntity entity = new PaymentOrderEntity();
        entity.setOrderNo("FV" + UUID.randomUUID()
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
        return new BlogCommentVO(entity.getId(), entity.getArticleId(), entity.getUsername(),
                entity.getContent(), entity.getCreatedAt());
    }

    private PaymentOrderVO toPaymentVO(PaymentOrderEntity entity) {
        return new PaymentOrderVO(entity.getId(), entity.getOrderNo(), entity.getPlanId(),
                entity.getPlanName(), entity.getAmountCents(), entity.getCurrency(), entity.getChannel(),
                entity.getStatus(), "/payment/demo/" + entity.getOrderNo(), entity.getCreatedAt(), entity.getExpiresAt());
    }

    private record Plan(String name, int amountCents) {
    }
}
