package com.fastvue.infrastructure.storage;

import static org.assertj.core.api.Assertions.*;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.infrastructure.tenant.TenantContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalFileStorageTest {
    @TempDir Path root;

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void generatedFileRoundTripsAndOtherTenantCannotReadOrDelete() throws Exception {
        var storage = new LocalFileStorage(root.toString(), "/files", 1024);
        TenantContext.set(1L, false);
        var file =
                storage.upload(
                        new MockMultipartFile(
                                "file", "note.txt", "text/plain", "hello".getBytes()));
        assertThat(storage.load(file.key()).getContentAsByteArray()).isEqualTo("hello".getBytes());
        TenantContext.set(2L, false);
        assertThatThrownBy(() -> storage.load(file.key())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> storage.delete(file.key())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> storage.load("2/../" + file.key()))
                .isInstanceOf(BusinessException.class);
        assertThat(Files.exists(root.resolve(file.key()))).isTrue();
    }

    @Test
    void missingMimeTypeReturnsValidationFailure() {
        TenantContext.set(1L, false);
        var storage = new LocalFileStorage(root.toString(), "/files", 1024);
        assertThatThrownBy(
                        () ->
                                storage.upload(
                                        new MockMultipartFile(
                                                "file", "file.txt", null, new byte[] {1})))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void symlinkCannotEscapeStorage() throws Exception {
        TenantContext.set(1L, false);
        var outside = Files.createDirectory(root.resolve("outside"));
        Files.createSymbolicLink(root.resolve("1"), outside);
        var storage = new LocalFileStorage(root.toString(), "/files", 1024);
        assertThatThrownBy(
                        () ->
                                storage.upload(
                                        new MockMultipartFile(
                                                "file", "file.txt", "text/plain", new byte[] {1})))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void failedCopyRemovesPartialFileAndPreservesExistingUploads() throws Exception {
        TenantContext.set(1L, false);
        var storage = new LocalFileStorage(root.toString(), "/files", 1024);
        byte[] content = "complete upload".getBytes(StandardCharsets.UTF_8);
        var existing =
                storage.upload(
                        new MockMultipartFile("file", "existing.txt", "text/plain", content));
        var broken =
                new MockMultipartFile("file", "broken.txt", "text/plain", content) {
                    private int opens;

                    @Override
                    public InputStream getInputStream() {
                        if (++opens == 1) return new ByteArrayInputStream(content);
                        return new InputStream() {
                            private boolean firstRead = true;

                            @Override
                            public int read() throws IOException {
                                if (firstRead) {
                                    firstRead = false;
                                    return 'x';
                                }
                                throw new IOException("Upload interrupted");
                            }
                        };
                    }
                };

        assertThatThrownBy(() -> storage.upload(broken))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件保存失败")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(com.fastvue.common.exception.ErrorCode.INTERNAL_ERROR);
        try (var files = Files.list(root.resolve("1"))) {
            assertThat(files.toList()).containsExactly(root.resolve(existing.key()));
        }
        assertThat(storage.load(existing.key()).getContentAsByteArray()).isEqualTo(content);
    }

    @Test
    void failedStreamCloseRemovesUnreportedUpload() throws Exception {
        TenantContext.set(1L, false);
        var storage = new LocalFileStorage(root.toString(), "/files", 1024);
        byte[] content = "complete upload".getBytes(StandardCharsets.UTF_8);
        var broken =
                new MockMultipartFile("file", "broken.txt", "text/plain", content) {
                    private int opens;

                    @Override
                    public InputStream getInputStream() {
                        if (++opens == 1) return new ByteArrayInputStream(content);
                        return new ByteArrayInputStream(content) {
                            @Override
                            public void close() throws IOException {
                                throw new IOException("Close failed");
                            }
                        };
                    }
                };

        assertThatThrownBy(() -> storage.upload(broken))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件保存失败");
        try (var files = Files.list(root.resolve("1"))) {
            assertThat(files.toList()).isEmpty();
        }
    }

    @Test
    void webpSignatureIsReadAsAsciiAndRejectsMismatchedContent() throws Exception {
        TenantContext.set(1L, false);
        var storage = new LocalFileStorage(root.toString(), "/files", 1024);
        byte[] header = "RIFFxxxxWEBP".getBytes(StandardCharsets.US_ASCII);
        var stored =
                storage.upload(new MockMultipartFile("file", "image.webp", "image/webp", header));
        assertThat(storage.load(stored.key()).getContentAsByteArray()).isEqualTo(header);
        assertThatThrownBy(
                        () ->
                                storage.upload(
                                        new MockMultipartFile(
                                                "file",
                                                "invalid.webp",
                                                "image/webp",
                                                "RIFFxxxxNOPE"
                                                        .getBytes(StandardCharsets.US_ASCII))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件内容与声明类型不一致");
    }
}
