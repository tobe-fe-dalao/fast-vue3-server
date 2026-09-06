package com.fastvue.module.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fastvue.common.BusinessException;
import com.fastvue.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        if (existsByNameOrSlug(request.name(), request.slug(), null)) {
            throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
        CategoryEntity entity = new CategoryEntity();
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
        if (existsByNameOrSlug(entity.getName(), entity.getSlug(), id)) {
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

    private boolean existsByNameOrSlug(String name, String slug, Long excludeId) {
        LambdaQueryWrapper<CategoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(CategoryEntity::getName, name).or().eq(CategoryEntity::getSlug, slug));
        if (excludeId != null) {
            wrapper.ne(CategoryEntity::getId, excludeId);
        }
        return categoryMapper.selectCount(wrapper) > 0;
    }
}
