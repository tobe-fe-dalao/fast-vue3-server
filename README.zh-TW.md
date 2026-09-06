# fast-vue3-server

**語言：** [English](./README.md) | [简体中文](./README.zh-CN.md) | 繁體中文（台灣） | [繁體中文（香港）](./README.zh-HK.md) | [日本語](./README.ja.md)

這是 Fast Vue3 的 Spring Boot 3 參考後端，以 Java 21、PostgreSQL、Redis、JWT、MyBatis-Plus 與 Flyway 提供統一的 `/api/v1` 契約。

線上文件：<https://tobe-fe-dalao.github.io/fast-vue3-site/zh-TW/server/>

## 不安裝 Java，直接使用 Docker 或 OrbStack

Docker Desktop 與 OrbStack 都提供相容的 Docker Compose CLI：

```bash
cp .env.example .env
docker compose --profile app up -d --build
docker compose ps
docker compose logs -f app
```

API 位於 `http://localhost:8080`，健康檢查是 `/actuator/health`，Swagger 是 `/swagger-ui.html`。開發帳號為 `admin / admin123`。

停止服務：

```bash
docker compose --profile app down
```

資料庫 volume 預設會保留；只有確定要刪除本機資料時才加上 `-v`。

## 本機 Java 開發

```bash
docker compose up -d
./mvnw spring-boot:run
```

本機模式需要 JDK 21。前端以 `VITE_DEV_BACKEND=server pnpm dev:site-antd` 或對應的 web/site 指令連接。

## API 安全邊界

登入、註冊、Token 更新和 `/api/v1/public/**` 內容可匿名使用。公開內容不會呼叫必須登入的 `SecurityUtils.currentUser()`；審計欄位使用 nullable 使用者 API。

評論列表可匿名讀取，發表評論與建立支付訂單需要 Access Token。Refresh Token 不能作為 Access Token。401 與 403 均使用 `{ code, message, data }` 格式。

`analytics:view` 與 `data:view` 分別保護分析與經營資料；`GET /api/v1/menus` 僅要求登入，因為它用來取得目前使用者自己的選單。

## 測試與文件

```bash
./mvnw test
```

JUnit、MockMvc 與 Testcontainers 驗證服務、權限、遷移和資料持久化。架構、設定、測試與 API 文件統一維護在 [fast-vue3-site](https://tobe-fe-dalao.github.io/fast-vue3-site/zh-TW/server/)；本倉庫不再包含 Node.js/VitePress 工具鏈。
