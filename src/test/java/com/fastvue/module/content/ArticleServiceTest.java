package com.fastvue.module.content;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.content.api.*;
import com.fastvue.module.content.persistence.*;
import com.fastvue.module.content.service.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {
    @Mock ArticleMapper articles;
    @Mock CategoryMapper categories;
    ArticleService service;

    @BeforeEach
    void setup() {
        TenantContext.set(1L, false);
        service =
                new ArticleService(
                        articles,
                        categories,
                        org.mapstruct.factory.Mappers.getMapper(ContentConverter.class));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    ArticleEntity article(long id, Long category) {
        var value = new ArticleEntity();
        value.setId(id);
        value.setTenantId(1L);
        value.setCategoryId(category);
        value.setTitle("Original");
        value.setContent(List.of("old"));
        value.setTags(List.of("tag"));
        return value;
    }

    @Test
    void paginationLoadsSharedCategoryOnceForAllArticles() {
        var page = new Page<ArticleEntity>(2, 10, 12);
        page.setRecords(List.of(article(2, 7L), article(3, 7L), article(4, null)));
        when(articles.selectPage(any(), any())).thenReturn(page);
        var category = new CategoryEntity();
        category.setId(7L);
        category.setName("News");
        when(categories.selectBatchIds(List.of(7L))).thenReturn(List.of(category));
        var result = service.page(new ArticleQueryRequest("news", "published", 7L, 2L, 10L));
        assertThat(result.getTotal()).isEqualTo(12);
        assertThat(result.getRecords())
                .extracting(ArticleVO::category)
                .containsExactly("News", "News", "");
        verify(categories).selectBatchIds(List.of(7L));
        verify(categories, never()).selectById(any(Long.class));
    }

    @Test
    void emptyPageDoesNotQueryCategories() {
        when(articles.selectPage(any(), any())).thenReturn(new Page<>());
        assertThat(service.page(new ArticleQueryRequest(null, null, null, null, null)).getRecords())
                .isEmpty();
        verifyNoInteractions(categories);
    }

    @Test
    void creationDefaultsToDraftAndEmptyLists() {
        doAnswer(
                        inv -> {
                            ArticleEntity value = inv.getArgument(0);
                            value.setId(8L);
                            when(articles.selectById(8L)).thenReturn(value);
                            return 1;
                        })
                .when(articles)
                .insert(any(ArticleEntity.class));
        var result =
                service.create(
                        new CreateArticleRequest(
                                "New", "Author", null, null, null, null, null, null));
        assertThat(result.title()).isEqualTo("New");
        assertThat(result.status()).isEqualTo("draft");
        assertThat(result.content()).isEmpty();
        assertThat(result.tags()).isEmpty();
        assertThat(result.date()).isNotBlank();
        verifyNoInteractions(categories);
    }

    @Test
    void categoryOutsideTenantIsRejectedBeforeWriting() {
        TenantContext.set(99L, true);
        when(categories.selectById(7L))
                .thenAnswer(
                        inv -> {
                            assertThat(TenantContext.tenantId()).isEqualTo(99L);
                            assertThat(TenantContext.isSuperAdmin()).isFalse();
                            return null;
                        });
        assertThatThrownBy(
                        () ->
                                service.create(
                                        new CreateArticleRequest(
                                                "New", null, 7L, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类");
        assertThat(TenantContext.isSuperAdmin()).isTrue();
        verifyNoInteractions(articles);
    }

    @Test
    void absentUpdateFieldsKeepValuesAndEmptyListsClearThem() {
        var value = article(8L, null);
        when(articles.selectById(8L)).thenReturn(value);
        var unchanged =
                service.update(
                        8L,
                        new UpdateArticleRequest(null, null, null, null, null, null, null, null));
        assertThat(unchanged.title()).isEqualTo("Original");
        assertThat(unchanged.content()).containsExactly("old");
        when(categories.selectById(7L)).thenReturn(new CategoryEntity());
        var changed =
                service.update(
                        8L,
                        new UpdateArticleRequest(
                                "Edited",
                                "Author",
                                7L,
                                List.of(),
                                "cover",
                                "published",
                                "summary",
                                List.of()));
        assertThat(changed.content()).isEmpty();
        assertThat(changed.tags()).isEmpty();
        assertThat(changed.title()).isEqualTo("Edited");
    }

    @Test
    void missingCategoryOnUpdateDoesNotWrite() {
        when(articles.selectById(8L)).thenReturn(article(8L, null));
        assertThatThrownBy(
                        () ->
                                service.update(
                                        8L,
                                        new UpdateArticleRequest(
                                                null, null, 9L, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class);
        verify(articles, never()).updateById(any(ArticleEntity.class));
    }

    @Test
    void missingArticleIsConsistentForGetUpdateAndDelete() {
        assertThatThrownBy(() -> service.getById(8L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(
                        () ->
                                service.update(
                                        8L,
                                        new UpdateArticleRequest(
                                                null, null, null, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.delete(8L)).isInstanceOf(BusinessException.class);
        verify(articles, never()).deleteById(any(Long.class));
    }

    @Test
    void logicalDeleteAndMissingCategoryDisplayAreSupported() {
        when(articles.selectById(8L)).thenReturn(article(8L, 7L));
        assertThat(service.getById(8L).category()).isEmpty();
        service.delete(8L);
        verify(articles).deleteById(8L);
    }
}
