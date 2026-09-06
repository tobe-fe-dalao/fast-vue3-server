# fast-vue3-server

**言語：** [English](./README.md) | [简体中文](./README.zh-CN.md) | [繁體中文（台灣）](./README.zh-TW.md) | [繁體中文（香港）](./README.zh-HK.md) | 日本語

[fast-vue3](https://github.com/tobe-fe-dalao/fast-vue3) の公式 Spring Boot 参照バックエンドです。Java 21 のモジュラーモノリスとして、すべての管理画面とサイトアプリに共通の `/api/v1` 契約を提供します。

オンラインドキュメント: <https://tobe-fe-dalao.github.io/fast-vue3-site/ja/server/>

## 技術構成

Spring Boot 3、Spring Security、JWT、MyBatis-Plus、PostgreSQL、Redis、Flyway、Springdoc OpenAPI、JUnit 5、MockMvc、Testcontainers を使用します。

## 起動

ローカル Java 開発では JDK 21 と Docker が必要です。

```bash
cp .env.example .env # 初回のみ
docker compose up -d
./mvnw spring-boot:run
```

ローカル JDK がない場合、Docker Desktop または OrbStack の互換 Compose CLI ですべてを起動できます。

```bash
docker compose --profile app up -d --build
docker compose ps
docker compose logs -f app
```

- Health: `http://localhost:8080/actuator/health`
- Swagger: `http://localhost:8080/swagger-ui.html`
- 開発アカウント: `admin / admin123`

## フロントエンド連携

```bash
cd /Users/fong/Workspace/personal/frontend/vue/fast-vue3/fast-vue3
VITE_DEV_BACKEND=server pnpm dev:web-antd
# または
VITE_DEV_BACKEND=server pnpm dev:site-antd
```

## API と認証境界

共通レスポンス：

```json
{ "code": 0, "message": "success", "data": {} }
```

ログイン、登録、トークン更新と `/api/v1/public/**` のコンテンツは公開です。ホーム、製品、価格、FAQ、ブログ、ブログ詳細、コメント一覧、お問い合わせはログインなしで利用できます。

次のユーザー操作だけは Access Token が必要です。

- `POST /api/v1/blog/{id}/comments`
- `POST /api/v1/payments/checkout`

支払い API は `alipay`、`wechat`、`card` を受け付け、`pending` の注文、金額、有効期限、`checkoutUrl` を返します。内蔵実装は実際の課金を行わないデモです。本番では決済事業者のセッション作成と署名付きコールバック検証に置き換えてください。

## データベースとテスト

Flyway の `V7__site_interactions.sql` がコメントと支払い注文テーブルを追加します。

```bash
./mvnw test
```

JUnit、MockMvc、Testcontainers でサービス、認証境界、PostgreSQL マイグレーションを検証します。

## ドキュメント

アーキテクチャ、セットアップ、設定、テスト、API リファレンスは [fast-vue3-site](https://tobe-fe-dalao.github.io/fast-vue3-site/ja/server/) で一元管理します。本リポジトリには簡潔な運用情報と Swagger/OpenAPI のみを残し、Node.js/VitePress のツールチェーンは含めません。

## ライセンス

[MIT](./LICENSE)
