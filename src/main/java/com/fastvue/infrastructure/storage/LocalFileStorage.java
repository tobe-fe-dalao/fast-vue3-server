package com.fastvue.infrastructure.storage;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.config.StorageProperties;
import com.fastvue.infrastructure.tenant.TenantContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalFileStorage implements FileStorage {
    private static final Map<String, Set<String>> ALLOWED =
            Map.of(
                    "image/png",
                    Set.of("png"),
                    "image/jpeg",
                    Set.of("jpg", "jpeg"),
                    "image/webp",
                    Set.of("webp"),
                    "application/pdf",
                    Set.of("pdf"),
                    "text/plain",
                    Set.of("txt"));

    private final Path root;
    private final String baseUrl;
    private final long maxSize;

    @Autowired
    public LocalFileStorage(StorageProperties properties) {
        this(properties.localRoot(), properties.publicBaseUrl(), properties.maxSizeBytes());
    }

    public LocalFileStorage(String root, String baseUrl, long maxSize) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
        this.maxSize = maxSize;
    }

    @Override
    public StoredFile upload(MultipartFile file) {
        String original = validate(file);
        String extension = extension(original);
        String key = TenantContext.tenantId() + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolve(key);
        try {
            Files.createDirectories(
                    Objects.requireNonNull(
                            target.getParent(), "Resolved upload must have a parent"));
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException cleanupFailure) {
                ex.addSuppressed(cleanupFailure);
            }
            var failure = new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
            failure.initCause(ex);
            throw failure;
        }
        return new StoredFile(key, original, file.getContentType(), file.getSize(), getUrl(key));
    }

    @Override
    public void delete(String key) {
        verifyTenantKey(key);
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件删除失败");
        }
    }

    @Override
    @SneakyThrows
    public Resource load(String key) {
        verifyTenantKey(key);
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) throw new BusinessException(ErrorCode.NOT_FOUND);
        return new UrlResource(path.toUri());
    }

    @Override
    public String getUrl(String key) {
        return baseUrl + "/" + key;
    }

    private String validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.FILE_REJECTED, "文件为空或超过大小限制");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.length() > 255 || original.contains("\0")) {
            throw new BusinessException(ErrorCode.FILE_REJECTED, "文件名不合法");
        }
        String type = file.getContentType();
        String ext = extension(original).toLowerCase(Locale.ROOT);
        if (!(type == null ? Set.<String>of() : ALLOWED.getOrDefault(type, Set.of()))
                .contains(ext)) {
            throw new BusinessException(ErrorCode.FILE_REJECTED, "文件类型或扩展名不允许");
        }
        try (var input = file.getInputStream()) {
            byte[] head = input.readNBytes(12);
            if (!signatureMatches(type, head))
                throw new BusinessException(ErrorCode.FILE_REJECTED, "文件内容与声明类型不一致");
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.FILE_REJECTED, "无法读取上传文件");
        }
        Path filename = Paths.get(original).getFileName();
        if (filename == null) throw new BusinessException(ErrorCode.FILE_REJECTED, "文件名不合法");
        return filename.toString();
    }

    private boolean signatureMatches(String type, byte[] b) {
        if ("text/plain".equals(type)) return true;
        if ("image/png".equals(type))
            return b.length >= 8
                    && b[0] == (byte) 0x89
                    && b[1] == 0x50
                    && b[2] == 0x4e
                    && b[3] == 0x47
                    && b[4] == 0x0d
                    && b[5] == 0x0a
                    && b[6] == 0x1a
                    && b[7] == 0x0a;
        if ("image/jpeg".equals(type))
            return b.length >= 3
                    && b[0] == (byte) 0xff
                    && b[1] == (byte) 0xd8
                    && b[2] == (byte) 0xff;
        if ("application/pdf".equals(type))
            return b.length >= 5
                    && b[0] == '%'
                    && b[1] == 'P'
                    && b[2] == 'D'
                    && b[3] == 'F'
                    && b[4] == '-';
        if ("image/webp".equals(type))
            return b.length >= 12
                    && new String(b, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(b, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
        return false;
    }

    private void verifyTenantKey(String key) {
        if (key == null || !key.matches("[1-9][0-9]*/[0-9a-fA-F-]{36}\\.[a-zA-Z0-9]+")) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!TenantContext.isSuperAdmin() && !key.startsWith(TenantContext.tenantId() + "/")) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    private Path resolve(String key) {
        verifyTenantKey(key);
        Path value = root.resolve(key).normalize();
        Path parent = value.getParent();
        if (parent == null
                || !value.startsWith(root)
                || Files.isSymbolicLink(root)
                || Files.isSymbolicLink(parent)
                || Files.isSymbolicLink(value)) {
            throw new BusinessException(ErrorCode.FILE_REJECTED, "非法文件路径");
        }
        return value;
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 1 || dot == name.length() - 1)
            throw new BusinessException(ErrorCode.FILE_REJECTED, "文件必须包含扩展名");
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
