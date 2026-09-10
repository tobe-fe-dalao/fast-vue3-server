package com.fastvue.infrastructure.storage;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.infrastructure.tenant.TenantContext;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Component
public class LocalFileStorage implements FileStorage {
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "image/png", Set.of("png"), "image/jpeg", Set.of("jpg", "jpeg"),
            "image/webp", Set.of("webp"), "application/pdf", Set.of("pdf"),
            "text/plain", Set.of("txt"));

    private final Path root;
    private final String baseUrl;
    private final long maxSize;

    public LocalFileStorage(@Value("${app.storage.local-root}") String root,
            @Value("${app.storage.public-base-url}") String baseUrl,
            @Value("${app.storage.max-size-bytes}") long maxSize) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
        this.maxSize = maxSize;
    }

    @Override
    public StoredFile upload(MultipartFile file) {
        validate(file);
        String original = Paths.get(Objects.requireNonNull(file.getOriginalFilename())).getFileName().toString();
        String extension = extension(original);
        String key = TenantContext.tenantId() + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
        }
        return new StoredFile(key, original, file.getContentType(), file.getSize(), getUrl(key));
    }

    @Override
    public void delete(String key) {
        verifyTenantKey(key);
        try { Files.deleteIfExists(resolve(key)); }
        catch (IOException ex) { throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件删除失败"); }
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
    public String getUrl(String key) { return baseUrl + "/" + key; }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.FILE_REJECTED, "文件为空或超过大小限制");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.length() > 255 || original.contains("\0")) {
            throw new BusinessException(ErrorCode.FILE_REJECTED, "文件名不合法");
        }
        String type = file.getContentType();
        String ext = extension(original).toLowerCase(Locale.ROOT);
        if (!ALLOWED.getOrDefault(type, Set.of()).contains(ext)) {
            throw new BusinessException(ErrorCode.FILE_REJECTED, "文件类型或扩展名不允许");
        }
        try {
            byte[] head = file.getInputStream().readNBytes(12);
            if (!signatureMatches(type, head)) throw new BusinessException(ErrorCode.FILE_REJECTED, "文件内容与声明类型不一致");
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.FILE_REJECTED, "无法读取上传文件");
        }
    }

    private boolean signatureMatches(String type, byte[] b) {
        if ("text/plain".equals(type)) return true;
        if ("image/png".equals(type)) return b.length >= 8 && b[0] == (byte)0x89 && b[1] == 0x50 && b[2] == 0x4e && b[3] == 0x47;
        if ("image/jpeg".equals(type)) return b.length >= 3 && b[0] == (byte)0xff && b[1] == (byte)0xd8 && b[2] == (byte)0xff;
        if ("application/pdf".equals(type)) return b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F';
        if ("image/webp".equals(type)) return b.length >= 12 && new String(b, 0, 4).equals("RIFF") && new String(b, 8, 4).equals("WEBP");
        return false;
    }

    private void verifyTenantKey(String key) {
        String prefix = TenantContext.tenantId() + "/";
        if (!TenantContext.isSuperAdmin() && (key == null || !key.startsWith(prefix))) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    private Path resolve(String key) {
        Path value = root.resolve(key == null ? "" : key).normalize();
        if (!value.startsWith(root)) throw new BusinessException(ErrorCode.FILE_REJECTED, "非法文件路径");
        return value;
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 1 || dot == name.length() - 1) throw new BusinessException(ErrorCode.FILE_REJECTED, "文件必须包含扩展名");
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
