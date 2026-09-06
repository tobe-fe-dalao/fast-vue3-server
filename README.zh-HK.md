# fast-vue3-server

**語言：** [English](./README.md) | [简体中文](./README.zh-CN.md) | [繁體中文（台灣）](./README.zh-TW.md) | 繁體中文（香港） | [日本語](./README.ja.md)

這是 Fast Vue3 的 Spring Boot 3 參考後端，以 Java 21、PostgreSQL、Redis、JWT、MyBatis-Plus 及 Flyway 提供統一 `/api/v1` 契約。

線上文件：<https://tobe-fe-dalao.github.io/fast-vue3-site/zh-HK/server/>

## 毋須安裝 Java，直接使用 Docker 或 OrbStack

Docker Desktop 及 OrbStack 都提供相容的 Docker Compose CLI：

```bash
cp .env.example .env
docker compose --profile app up -d --build
docker compose ps
docker compose logs -f app
```

API 位於 `http://localhost:8080`，健康檢查是 `/actuator/health`，Swagger 是 `/swagger-ui.html`。開發帳戶為 `admin / admin123`。

停止服務：

```bash
docker compose --profile app down
```

資料庫 volume 預設保留；只有確定要刪除本機資料時才加入 `-v`。

## 本機 Java 開發

```bash
docker compose up -d
./mvnw spring-boot:run
```

本機模式需要 JDK 21。前端使用 `VITE_DEV_BACKEND=server pnpm dev:site-antd` 或對應 web/site 指令連接。

## API 安全界線

登入、註冊、Token 更新及 `/api/v1/public/**` 內容可匿名使用。公開內容不會呼叫必須登入的 `SecurityUtils.currentUser()`；審計欄位使用 nullable 用戶 API。

評論清單可匿名讀取，發表評論及建立付款訂單需要 Access Token。Refresh Token 不能當作 Access Token。401 及 403 均使用 `{ code, message, data }` 格式。

`analytics:view` 及 `data:view` 分別保護分析及營運資料；`GET /api/v1/menus` 只要求登入，因為它用來取得目前用戶自己的選單。

## 測試及文件

```bash
./mvnw test
```

JUnit、MockMvc 及 Testcontainers 驗證服務、權限、遷移及資料持久化。架構、設定、測試及 API 文件統一維護在 [fast-vue3-site](https://tobe-fe-dalao.github.io/fast-vue3-site/zh-HK/server/)；本倉庫不再包含 Node.js/VitePress 工具鏈。
