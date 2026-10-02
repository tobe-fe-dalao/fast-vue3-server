# fast-vue3-server

[English](./README.md) · [简体中文](./README.zh-CN.md) · [繁體中文（台灣）](./README.zh-TW.md) · [繁體中文（香港）](./README.zh-HK.md) · [日本語](./README.ja.md)

Fast Vue3 の Java バックエンド。共有 `/api/v1` 契約を提供します。

Java 21、Spring Boot 3.5.16、Spring MVC/Security、MyBatis-Plus、PostgreSQL 16、Redis 7、Flyway、JUnit 5、Testcontainers を使用します。`module/<domain>/{api,service,persistence}` を維持し、Controller は DTO を返し、Service が業務とトランザクションを管理します。

[Documentation](https://tobe-fe-dalao.github.io/fast-vue3-site/ja/server/) · [Configuration](https://tobe-fe-dalao.github.io/fast-vue3-site/ja/server/guide/configuration) · [API contracts](https://tobe-fe-dalao.github.io/fast-vue3-site/ja/server/api/contracts)

## ローカル開発

```bash
cp .env.example .env
docker compose --profile app up -d --build
```

Java 21:

```bash
docker compose up -d
set -a
source .env
set +a
./mvnw spring-boot:run
```

- API: `http://localhost:8080/api/v1`
- Health: `http://localhost:8080/actuator/health`
- Swagger: `http://localhost:8080/swagger-ui.html` (development)
- Development only: `admin / admin123`

## ビルドとテスト

```bash
./mvnw spotless:apply
./mvnw spotless:check checkstyle:check
./mvnw -DskipTests test-compile pmd:check spotbugs:check
./mvnw test
./mvnw clean verify
```

役割分担：Spotless 3.10.4 は google-java-format 1.28.0 の AOSP（4 スペース）形式とインポート整理を担当します。Checkstyle は既存の基本規約を検査し、Maven PMD 3.28.0（PMD 7.17.0）は本体とテストのリソース管理、未使用コード、認知的複雑度（メソッドのしきい値 25）を検査します。SpotBugs 4.10.5.0 は既定の信頼度で本体のバイトコードを解析します。Error Prone 2.43.0 は Java 21 のコンパイル時に設定済みの 21 項目を検査し、Lombok/MapStruct と併用します。

`validate` で形式と Checkstyle、本体・テストのコンパイル時に Error Prone、`verify` で PMD/SpotBugs を実行します。ビルドはソースを書き換えません。整形は `spotless:apply` で明示的に実行します。静的検査に Docker は不要で、生成コードは対象外です。ルールは `config/` にあり、CI は `target/checkstyle-result.xml`、`target/pmd.xml`、`target/spotbugsXml.xml` を保存します。

除外は型、メソッド、警告種別を限定しています。列挙した既存のリクエスト／レスポンス DTO ではコレクション参照の扱いを維持し、共有 Spring Bean のコンストラクタ引数、検証済みリクエスト ID の応答ヘッダー、CORS の条件付き防御的コピーには個別の理由を記載しています。PMD は `finally` 内のスレッドプール停止を認識し、Mockito の JDBC テストダブルだけでリソース解放の警告を局所的に除外します。他の null、リソース、バグ検出は有効です。

`test-compile` は本番コードとテストコードの型をコンパイル時に検査します。未検査変換、raw 型、非推奨 API の警告、MapStruct の変換先フィールドの対応漏れはコンパイルエラーになります。`test` は Docker なしで JUnit/Mockito/MockMvc を実行します。`clean verify` はさらに Failsafe で PostgreSQL/Redis の結合テストを実行して Jar を生成します。Docker が必須で、環境がない場合はスキップせず失敗します。JaCoCo の統合カバレッジは `target/site/jacoco/index.html` と `jacoco.xml` に出力されます。ローカル検証はリモート CI の結果を保証しません。

## Production

本番は独立した `compose.prod.yml` を使用し、開発 Compose と統合しないでください。秘密の `.env.prod` またはシークレット管理で必須設定を渡します。JWT のランダム鍵は32バイト以上、初期パスワードは12文字以上かつ UTF-8 で72バイト以下にします。CORS は明示した HTTPS Origin のみ許可します。開発 Profile、仮の鍵、デモアダプターは起動時に拒否されます。

```bash
cp .env.prod.example .env.prod
# Supply required secrets and explicit HTTPS origins before running.
docker compose --env-file .env.prod -f compose.prod.yml up -d --build
```

Health と liveness/readiness は匿名で読めますが、インフラの詳細は非公開です。Readiness は PostgreSQL と Redis、liveness はアプリ状態のみを確認します。Metrics と Prometheus には `audit:view` が必要です。本番 ECS JSON ログにはリクエスト ID、テナント/ユーザー、HTTP 遅延を含め、秘密ヘッダーや本文は記録しません。Trace 出力は未設定であり、完全な OpenTelemetry トレースではありません。

本番ではデモのダッシュボード、分析、ポータル固定データを無効化します。デモ決済は注文を保存せず503を返します。実決済事業者、署名付きコールバック、台帳と決済冪等性は業務連携が必要で、現在の処理で実際の資金を受け取ることはできません。

## 実行時の制限と通知の復旧

- ロック待機は 5 秒、SQL は 15 秒、トランザクションは 20 秒が既定値です。`DB_LOCK_TIMEOUT_MILLIS`、`DB_STATEMENT_TIMEOUT_SECONDS`、`DB_TRANSACTION_TIMEOUT_SECONDS` で変更し、起動時に大小関係を検証します。トランザクションのタイムアウトは HTTP リクエスト全体の期限ではありません。
- ストレージと Servlet は `FILE_MAX_SIZE_BYTES`（10 MiB）を共有します。`FILE_MAX_REQUEST_SIZE_BYTES` は既定で 11 MiB、multipart 用に最低 64 KiB の余裕が必要です。Tomcat も同じ有限サイズまで拒否したアップロードの残りを読み取り、その範囲内ではクライアントが 413 応答を受信できます。`file` の欠落は 400、サイズ超過は 413 を返します。
- ID 配列は null 要素を除く最大 1000 件で、重複するロール権限 ID は統合します。記事は最大 1000 段落、各 10000 文字、タグは最大 100 件、各 64 文字です。更新時の配列省略は値を保持し、空配列は消去します。
- 通知は業務トランザクション内で PostgreSQL outbox に保存し、コミット直後に配信を試みます。バックグラウンド処理は既定で 5 秒ごとに 20 件です。通知の保存と配信確認を同一トランザクションで行い、行ロックで複数インスタンスを調整します。失敗時は 5 秒から最大 1 時間まで指数バックオフし、既定で 10 回後に `FAILED` として保持します。`NOTIFICATION_BATCH_SIZE`、`NOTIFICATION_MAX_ATTEMPTS`、`NOTIFICATION_POLL_INTERVAL_MS` で調整できます。
- `sys_notification_outbox` の `status/attempts/last_error` を確認します。エラーには例外クラス名だけを保存します。原因を解消したら、対象 UUID の `FAILED` 行だけを `status='PENDING', attempts=0, next_attempt_at=now(), last_error=NULL` に更新します。配信済みの行は再送しません。保存方針に従って完了履歴を削除し、未完了行は保持してください。
- V12 はテナント内の有効なカテゴリ名に一意索引を追加します。適用前に `SELECT tenant_id,name,count(*) FROM content_category WHERE NOT deleted GROUP BY tenant_id,name HAVING count(*) > 1;` で重複を確認し、業務担当者と解消してください。重複があれば移行は失敗し、名前変更やデータ削除は行いません。V13 は通知 outbox を追加します。
- `clean verify` は記事、カテゴリ、メニュー、レート制限、通知配信の各サービスクラスに行 90%・分岐 70%、部門サービスに行 85%・分岐 70% のカバレッジを要求します。他のモジュールもレポート対象です。

[Architecture and enterprise API](https://tobe-fe-dalao.github.io/fast-vue3-site/ja/server/api/enterprise): thin Controller → Service → MyBatis Mapper; DTO/persistence boundaries, tenant isolation, resource ownership, service transactions, optimistic locking and durable command idempotency. Schema changes are append-only Flyway migrations. Shared API changes must include frontend clients, Nitro mock and tests. Do not commit credentials or generated files.
