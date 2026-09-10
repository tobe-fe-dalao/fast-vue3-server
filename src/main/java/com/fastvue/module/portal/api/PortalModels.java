package com.fastvue.module.portal.api;

import java.util.List;

/**
 * 门户站公开接口响应模型。
 *
 * <p>字段结构与前端 {@code types.ts} 中的门户契约类型一一对应。</p>
 */
public final class PortalModels {

    private PortalModels() {
    }

    /** 门户博客文章 */
    public record BlogPost(
            String author,
            String category,
            List<String> content,
            String cover,
            String date,
            String excerpt,
            long id,
            List<String> tags,
            String title) {
    }

    /** 博客列表（含分类筛选器） */
    public record BlogListResult(
            List<BlogPost> items,
            long page,
            long pageSize,
            long total,
            List<String> categories) {
    }

    /** 常见问答 */
    public record FaqItem(long id, String question, String answer, String category) {
    }

    /** 套餐方案 */
    public record PricingPlan(
            long id,
            String name,
            String description,
            String price,
            String period,
            List<String> features,
            boolean highlighted) {
    }

    /** 功能特性 */
    public record FeatureItem(long id, String title, String description, String icon) {
    }

    /** 高亮信息块（产品/首页共用） */
    public record Highlight(String title, String description) {
    }

    /** 数值统计块（关于/首页共用） */
    public record Stat(String label, String value) {
    }

    /** 团队成员 */
    public record Team(String name, String role, String avatar) {
    }

    /** 发展历程里程碑 */
    public record Milestone(String date, String title, String description) {
    }

    /** 关于我们 */
    public record AboutInfo(
            String intro,
            List<Stat> stats,
            List<Team> team,
            List<Milestone> milestones) {
    }

    /** 产品信息 */
    public record ProductInfo(String name, String slogan, List<Highlight> highlights) {
    }

    /** 文档条目 */
    public record DocItem(String path, String title) {
    }

    /** 文档目录分区 */
    public record DocSection(long id, String title, String description, List<DocItem> items) {
    }

    /** 首页信息 */
    public record HomeInfo(
            List<Highlight> highlights,
            List<Stat> stats,
            List<Testimonial> testimonials) {
    }

    /** 用户评价 */
    public record Testimonial(String content, String name, String role) {
    }

    /** 联系表单参数 */
    public record ContactParams(String email, String message, String name) {
    }

    /** 联系表单提交结果 */
    public record ContactResult(boolean accepted, String email, String submittedAt) {
    }
}
