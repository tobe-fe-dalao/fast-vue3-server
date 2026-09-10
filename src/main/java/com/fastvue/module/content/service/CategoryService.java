package com.fastvue.module.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.module.content.api.CategoryVO;
import com.fastvue.module.content.api.CreateCategoryRequest;
import com.fastvue.module.content.api.UpdateCategoryRequest;
import com.fastvue.module.content.persistence.CategoryEntity;
import com.fastvue.module.content.persistence.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.fastvue.infrastructure.tenant.TenantContext;

/**
 * 分类业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final ContentConverter contentConverter;

    /**
     * 返回全部分类（不分页）。
     */
    public List<CategoryVO> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CategoryEntity>().orderByAsc(CategoryEntity::getId))
                .stream()
                .map(contentConverter::toCategoryVO)
                .toList();
    }

    /**
     * 查询单个分类。
     */
    public CategoryVO getById(Long id) {
        CategoryEntity entity = categoryMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return contentConverter.toCategoryVO(entity);
    }

    /**
     * 创建分类（校验名称/标识唯一性）。
     */
    @Transactional
    public CategoryVO create(CreateCategoryRequest request) {
        if (existsByNameOrSlug(request.name(), request.slug(), null, TenantContext.tenantId())) {
            throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
        CategoryEntity entity = new CategoryEntity();
        entity.setTenantId(TenantContext.tenantId());
        entity.setName(request.name());
        entity.setSlug(request.slug());
        entity.setDescription(request.description());
        entity.setStatus(request.status() != null ? request.status() : "active");
        categoryMapper.insert(entity);
        return getById(entity.getId());
    }

    /**
     * 更新分类。
     */
    @Transactional
    public CategoryVO update(Long id, UpdateCategoryRequest request) {
        CategoryEntity entity = categoryMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.slug() != null) {
            entity.setSlug(request.slug());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        if (existsByNameOrSlug(entity.getName(), entity.getSlug(), id, entity.getTenantId())) {
            throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
        categoryMapper.updateById(entity);
        return getById(id);
    }

    /**
     * 删除分类（逻辑删除）。
     */
    @Transactional
    public void delete(Long id) {
        if (categoryMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        categoryMapper.deleteById(id);
    }

    private boolean existsByNameOrSlug(String name, String slug, Long excludeId, Long tenantId) {
        LambdaQueryWrapper<CategoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CategoryEntity::getTenantId, tenantId);
        wrapper.and(w -> w.eq(CategoryEntity::getName, name).or().eq(CategoryEntity::getSlug, slug));
        if (excludeId != null) {
            wrapper.ne(CategoryEntity::getId, excludeId);
        }
        return categoryMapper.selectCount(wrapper) > 0;
    }
}
