package com.fastvue.module.content.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

// JDBC objects in this fixture are Mockito mocks; no connections or native resources are opened.
@SuppressWarnings("PMD.CloseResource")
class JsonStringListTypeHandlerTest {
    private final JsonStringListTypeHandler handler = new JsonStringListTypeHandler();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "null", "[]"})
    void absentListIsAlwaysAnEmptyCollection(String stored) throws SQLException {
        ResultSet result = mock(ResultSet.class);
        when(result.getString("tags")).thenReturn(stored);
        assertThat(handler.getNullableResult(result, "tags")).isEmpty();
    }

    @Test
    void indexedJdbcReadsAlsoNormalizeJsonNull() throws SQLException {
        ResultSet result = mock(ResultSet.class);
        CallableStatement statement = mock(CallableStatement.class);
        when(result.getString(2)).thenReturn("null");
        when(statement.getString(3)).thenReturn("null");
        assertThat(handler.getNullableResult(result, 2)).isEmpty();
        assertThat(handler.getNullableResult(statement, 3)).isEmpty();
    }

    @Test
    void unicodeAndEscapedStringsRoundTripThroughJdbc() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);
        List<String> values = List.of("Vue 与 Java", "a\"b", "line\nbreak");
        handler.setNonNullParameter(statement, 1, values, JdbcType.VARCHAR);
        var json = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(statement).setString(eq(1), json.capture());
        ResultSet result = mock(ResultSet.class);
        when(result.getString("content")).thenReturn(json.getValue());
        assertThat(handler.getNullableResult(result, "content")).containsExactlyElementsOf(values);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "{}", "[\"unclosed\""})
    void malformedStorageRaisesSqlExceptionInsteadOfSilentlyDroppingContent(String stored)
            throws SQLException {
        ResultSet result = mock(ResultSet.class);
        when(result.getString("content")).thenReturn(stored);
        assertThatThrownBy(() -> handler.getNullableResult(result, "content"))
                .isInstanceOf(SQLException.class)
                .hasCauseInstanceOf(Exception.class);
    }
}
