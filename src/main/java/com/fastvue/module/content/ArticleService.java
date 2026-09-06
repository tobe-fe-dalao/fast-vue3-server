package com.fastvue.module.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fastvue.common.BusinessException;
import com.fastvue.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文章业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final ContentConverter contentConverter;

    /**
     * 分页查询文章，支持标题/作者/摘要模糊匹配、状态与分类过滤。
     */
    public Page<ArticleVO> page(ArticleQueryRequest query) {
        LambdaQueryWrapper<ArticleEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(w -> w.like(ArticleEntity::getTitle, query.keyword())
                    .or().like(ArticleEntity::getAuthor, query.keyword())
                    .or().like(ArticleEntity::getSummary, query.keyword()));
        }
        if (StringUtils.hasText(query.status())) {
            wrapper.eq(ArticleEntity::getStatus, query.status());
        }
        if (query.categoryId() != null) {
            wrapper.eq(ArticleEntity::getCategoryId, query.categoryId());
        }
        wrapper.orderByDesc(ArticleEntity::getId);

        Page<ArticleEntity> page = articleMapper.selectPage(new Page<>(query.page(), query.pageSize()), wrapper);
        Map<Long, String> categoryNames = loadCategoryNames();
        Page<ArticleVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(entity -> toVO(entity, categoryNames))
                .toList());
        return result;
    }

    /**
     * 查询单篇文章。
     */
    public ArticleVO getById(Long id) {
        ArticleEntity entity = articleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        return toVO(entity, loadCategoryNames());
    }

    /**
     * 创建文章。
     */
    @Transactional
    public ArticleVO create(CreateArticleRequest request) {
        ArticleEntity entity = new ArticleEntity();
        entity.setTitle(request.title());
        entity.setAuthor(request.author());
        entity.setSummary(request.summary());
        entity.setContent(request.content() != null ? request.content() : List.of());
        entity.setCover(request.cover());
        entity.setCategoryId(request.categoryId());
        entity.setStatus(request.status() != null ? request.status() : "draft");
        entity.setTags(request.tags() != null ? request.tags() : List.of());
        entity.setDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        articleMapper.insert(entity);
        return getById(entity.getId());
    }

    /**
     * 更新文章。
     */
    @Transactional
    public ArticleVO update(Long id, UpdateArticleRequest request) {
        ArticleEntity entity = articleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        if (request.title() != null) {
            entity.setTitle(request.title());
        }
        if (request.author() != null) {
            entity.setAuthor(request.author());
        }
        if (request.summary() != null) {
            entity.setSummary(request.summary());
        }
        if (request.content() != null) {
            entity.setContent(request.content());
        }
        if (request.cover() != null) {
            entity.setCover(request.cover());
        }
        if (request.categoryId() != null) {
            entity.setCategoryId(request.categoryId());
        }
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        if (request.tags() != null) {
            entity.setTags(request.tags());
        }
        articleMapper.updateById(entity);
        return getById(id);
    }

    /**
     * 删除文章（逻辑删除）。
     */
    @Transactional
    public void delete(Long id) {
        if (articleMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        articleMapper.deleteById(id);
    }

    private ArticleVO toVO(ArticleEntity entity, Map<Long, String> categoryNames) {
        ArticleVO vo = contentConverter.toArticleVO(entity);
        String category = entity.getCategoryId() != null
                ? categoryNames.getOrDefault(entity.getCategoryId(), "") : "";
        return new ArticleVO(vo.id(), vo.author(), category, vo.categoryId(), vo.content(),
                vo.cover(), vo.date(), vo.status(), vo.summary(), vo.tags(), vo.title());
    }

    private Map<Long, String> loadCategoryNames() {
        return categoryMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .collect(Collectors.toMap(CategoryEntity::getId, CategoryEntity::getName,
                        (a, b) -> a, LinkedHashMap::new));
    }
}
