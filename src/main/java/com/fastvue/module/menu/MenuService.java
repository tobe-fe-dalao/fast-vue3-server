package com.fastvue.module.menu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fastvue.common.BusinessException;
import com.fastvue.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuMapper menuMapper;
    private final MenuConverter menuConverter;

    /**
     * 查询全部菜单（树形结构）。
     */
    public List<MenuVO> tree() {
        List<MenuEntity> entities = menuMapper.selectList(
                new LambdaQueryWrapper<MenuEntity>()
                        .orderByAsc(MenuEntity::getSort)
                        .orderByAsc(MenuEntity::getId));
        return buildTree(entities, 0L);
    }

    /**
     * 查询指定菜单 id 集合构成的树。
     *
     * <p>用于「当前用户有权限访问的菜单树」：传入用户可访问的菜单 id，仅返回这些菜单。</p>
     */
    public List<MenuVO> treeByIds(List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return List.of();
        }
        List<MenuEntity> entities = menuMapper.selectList(
                new LambdaQueryWrapper<MenuEntity>()
                        .in(MenuEntity::getId, menuIds)
                        .orderByAsc(MenuEntity::getSort)
                        .orderByAsc(MenuEntity::getId));
        return buildTree(entities, 0L);
    }

    /**
     * 查询单个菜单。
     */
    public MenuVO getById(Long id) {
        MenuEntity entity = menuMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        return menuConverter.toVO(entity);
    }

    /**
     * 创建菜单。
     */
    public MenuVO create(MenuRequest request) {
        MenuEntity entity = new MenuEntity();
        apply(entity, request);
        menuMapper.insert(entity);
        return menuConverter.toVO(entity);
    }

    /**
     * 更新菜单。
     */
    public MenuVO update(Long id, MenuRequest request) {
        MenuEntity entity = menuMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        apply(entity, request);
        menuMapper.updateById(entity);
        return menuConverter.toVO(entity);
    }

    /**
     * 删除菜单（逻辑删除）。有子菜单时拒绝删除。
     */
    @Transactional
    public void delete(Long id) {
        if (menuMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        Long childCount = menuMapper.selectCount(
                new LambdaQueryWrapper<MenuEntity>().eq(MenuEntity::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "存在子菜单，无法删除");
        }
        menuMapper.deleteById(id);
    }

    private void apply(MenuEntity entity, MenuRequest request) {
        entity.setParentId(request.parentId());
        entity.setName(request.name());
        entity.setPath(request.path());
        entity.setComponent(request.component());
        entity.setIcon(request.icon());
        entity.setSort(request.sort());
        entity.setVisible(request.visible());
        entity.setPermission(request.permission());
        entity.setType(request.type());
    }

    /**
     * 将扁平菜单列表组装为树。
     */
    private List<MenuVO> buildTree(List<MenuEntity> entities, Long parentId) {
        Map<Long, List<MenuEntity>> byParent = entities.stream()
                .collect(Collectors.groupingBy(MenuEntity::getParentId));

        return byParent.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(MenuEntity::getSort)
                        .thenComparing(MenuEntity::getId))
                .map(entity -> {
                    MenuVO vo = menuConverter.toVO(entity);
                    List<MenuVO> children = buildTree(entities, entity.getId());
                    return new MenuVO(
                            vo.id(), vo.parentId(), vo.name(), vo.path(), vo.component(),
                            vo.icon(), vo.sort(), vo.visible(), vo.permission(), vo.type(),
                            children.isEmpty() ? new ArrayList<>() : children);
                })
                .toList();
    }
}
