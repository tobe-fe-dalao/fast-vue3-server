package com.fastvue.module.content;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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
class CategoryServiceTest {
    @Mock CategoryMapper mapper;
    CategoryService service;

    @BeforeEach
    void setup() {
        TenantContext.set(1L, false);
        service =
                new CategoryService(
                        mapper, org.mapstruct.factory.Mappers.getMapper(ContentConverter.class));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    CategoryEntity category() {
        var value = new CategoryEntity();
        value.setId(8L);
        value.setTenantId(1L);
        value.setName("News");
        value.setSlug("news");
        value.setStatus("active");
        return value;
    }

    @Test
    void creationDefaultsStatusAndReadsPersistedValue() {
        doAnswer(
                        inv -> {
                            CategoryEntity value = inv.getArgument(0);
                            value.setId(8L);
                            when(mapper.selectById(8L)).thenReturn(value);
                            return 1;
                        })
                .when(mapper)
                .insert(any(CategoryEntity.class));
        var value = service.create(new CreateCategoryRequest("News", "news", "description", null));
        assertThat(value.name()).isEqualTo("News");
        assertThat(value.status()).isEqualTo("active");
    }

    @Test
    void duplicateNameOrSlugRejectsCreateAndUpdate() {
        when(mapper.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(
                        () -> service.create(new CreateCategoryRequest("News", "news", null, null)))
                .isInstanceOf(BusinessException.class);
        when(mapper.selectById(8L)).thenReturn(category());
        assertThatThrownBy(
                        () ->
                                service.update(
                                        8L, new UpdateCategoryRequest("News", null, null, null)))
                .isInstanceOf(BusinessException.class);
        verify(mapper, never()).insert(any(CategoryEntity.class));
        verify(mapper, never()).updateById(any(CategoryEntity.class));
    }

    @Test
    void updatesPreserveAbsentFieldsAndApplyProvidedValues() {
        when(mapper.selectById(8L)).thenReturn(category());
        var unchanged = service.update(8L, new UpdateCategoryRequest(null, null, null, null));
        assertThat(unchanged.name()).isEqualTo("News");
        // Return the same persisted object for the reload.
        var entity = category();
        when(mapper.selectById(8L)).thenReturn(entity);
        var changed =
                service.update(
                        8L,
                        new UpdateCategoryRequest("Updated", "updated", "description", "disabled"));
        assertThat(changed.name()).isEqualTo("Updated");
        assertThat(changed.slug()).isEqualTo("updated");
        assertThat(changed.status()).isEqualTo("disabled");
    }

    @Test
    void missingCategoryIsConsistentForGetUpdateAndDelete() {
        assertThatThrownBy(() -> service.getById(8L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(
                        () -> service.update(8L, new UpdateCategoryRequest(null, null, null, null)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.delete(8L)).isInstanceOf(BusinessException.class);
        verify(mapper, never()).deleteById(any(Long.class));
    }

    @Test
    void listingAndLogicalDeletionUseTheMapper() {
        when(mapper.selectList(any())).thenReturn(List.of(category()));
        when(mapper.selectById(8L)).thenReturn(category());
        assertThat(service.listAll()).extracting(CategoryVO::name).containsExactly("News");
        service.delete(8L);
        verify(mapper).deleteById(8L);
    }
}
