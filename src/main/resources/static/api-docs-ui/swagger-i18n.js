(function () {
  "use strict";

  const supportedLanguages = ["zh-CN", "ja-JP", "en-US"];
  const pageMessages = {
    "zh-CN": {
      pageTitle: "Fast Vue3 API 文档",
      pageSubtitle: "接口说明、在线调试与数据模型",
      language: "页面语言",
      openJson: "查看 OpenAPI JSON",
      authHint: "公开接口可直接调用；其他接口请先登录，再点击“授权”填入 Bearer Token。",
      loading: "正在加载接口文档……",
      loadFailed: "接口文档加载失败，请确认服务已启动并检查对应 JSON 地址。"
    },
    "ja-JP": {
      pageTitle: "Fast Vue3 API ドキュメント",
      pageSubtitle: "API の説明、オンライン実行、データモデル",
      language: "表示言語",
      openJson: "OpenAPI JSON を表示",
      authHint: "公開 API はそのまま実行できます。その他の API はログイン後、「認証」から Bearer Token を入力してください。",
      loading: "API ドキュメントを読み込んでいます……",
      loadFailed: "API ドキュメントを読み込めません。サーバーと JSON URL を確認してください。"
    },
    "en-US": {
      pageTitle: "Fast Vue3 API Documentation",
      pageSubtitle: "API reference, interactive requests, and data models",
      language: "Language",
      openJson: "View OpenAPI JSON",
      authHint: "Public endpoints can be called directly. For other endpoints, sign in and enter the Bearer Token under Authorize.",
      loading: "Loading API documentation…",
      loadFailed: "Failed to load the API documentation. Check that the server and JSON URL are available."
    }
  };

  const swaggerMessages = {
    "zh-CN": {
      "Authorize": "授权",
      "Close": "关闭",
      "Available authorizations": "可用的授权方式",
      "Name:": "名称：",
      "In:": "位置：",
      "Value:": "值：",
      "Logout": "退出授权",
      "Try it out": "在线调试",
      "Cancel": "取消",
      "Execute": "执行",
      "Clear": "清空",
      "Parameters": "请求参数",
      "No parameters": "无请求参数",
      "Request body": "请求体",
      "Responses": "响应",
      "Response content type": "响应内容类型",
      "Code": "状态码",
      "Description": "说明",
      "Links": "链接",
      "Response body": "响应体",
      "Response headers": "响应头",
      "Request URL": "请求地址",
      "Server response": "服务器响应",
      "Curl": "Curl 命令",
      "Download": "下载",
      "Schemas": "数据模型",
      "Models": "数据模型",
      "Example Value": "示例值",
      "Model": "模型",
      "Model Schema": "模型结构",
      "Required": "必填",
      "Default": "默认值",
      "Send empty value": "发送空值",
      "Deprecated": "已弃用",
      "Loading": "加载中",
      "Failed to load API definition.": "无法加载 API 定义。",
      "Possible values:": "可选值：",
      "minimum:": "最小值：",
      "maximum:": "最大值：",
      "pattern:": "格式：",
      "Filter by tag": "按标签筛选",
      "Expand operation": "展开接口",
      "Collapse operation": "收起接口"
    },
    "ja-JP": {
      "Authorize": "認証",
      "Close": "閉じる",
      "Available authorizations": "利用可能な認証方式",
      "Name:": "名前：",
      "In:": "場所：",
      "Value:": "値：",
      "Logout": "認証を解除",
      "Try it out": "試してみる",
      "Cancel": "キャンセル",
      "Execute": "実行",
      "Clear": "クリア",
      "Parameters": "パラメーター",
      "No parameters": "パラメーターなし",
      "Request body": "リクエストボディ",
      "Responses": "レスポンス",
      "Response content type": "レスポンスのコンテンツタイプ",
      "Code": "コード",
      "Description": "説明",
      "Links": "リンク",
      "Response body": "レスポンスボディ",
      "Response headers": "レスポンスヘッダー",
      "Request URL": "リクエスト URL",
      "Server response": "サーバーレスポンス",
      "Curl": "Curl コマンド",
      "Download": "ダウンロード",
      "Schemas": "スキーマ",
      "Models": "モデル",
      "Example Value": "値の例",
      "Model": "モデル",
      "Model Schema": "モデルスキーマ",
      "Required": "必須",
      "Default": "デフォルト",
      "Send empty value": "空の値を送信",
      "Deprecated": "非推奨",
      "Loading": "読み込み中",
      "Failed to load API definition.": "API 定義を読み込めません。",
      "Possible values:": "選択可能な値：",
      "minimum:": "最小値：",
      "maximum:": "最大値：",
      "pattern:": "パターン：",
      "Filter by tag": "タグで絞り込む",
      "Expand operation": "API を展開",
      "Collapse operation": "API を折りたたむ"
    },
    "en-US": {}
  };

  function currentLanguage() {
    const requested = new URLSearchParams(window.location.search).get("lang");
    if (supportedLanguages.includes(requested)) {
      return requested;
    }
    const saved = window.localStorage.getItem("fastvue.swagger.language");
    return supportedLanguages.includes(saved) ? saved : "zh-CN";
  }

  function localizePage(language) {
    const messages = pageMessages[language];
    document.documentElement.lang = language;
    document.title = messages.pageTitle;
    document.querySelectorAll("[data-i18n]").forEach(function (element) {
      const key = element.dataset.i18n;
      if (messages[key]) {
        element.textContent = messages[key];
      }
    });
    const selector = document.getElementById("language-select");
    selector.value = language;
    selector.setAttribute("aria-label", messages.language);
    document.getElementById("openapi-json-link").href = "/v3/api-docs/" + language;
  }

  function replaceText(text, translations) {
    const trimmed = text.trim();
    if (!translations[trimmed]) {
      return text;
    }
    return text.replace(trimmed, translations[trimmed]);
  }

  function localizeSwaggerNode(root, language) {
    const translations = swaggerMessages[language];
    if (!translations || language === "en-US") {
      return;
    }

    const elements = root.nodeType === Node.ELEMENT_NODE ? [root] : [];
    if (root.querySelectorAll) {
      elements.push.apply(elements, root.querySelectorAll("*"));
    }
    elements.forEach(function (element) {
      ["placeholder", "title", "aria-label"].forEach(function (attribute) {
        const value = element.getAttribute && element.getAttribute(attribute);
        if (value && translations[value]) {
          element.setAttribute(attribute, translations[value]);
        }
      });
    });

    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    const nodes = [];
    while (walker.nextNode()) {
      nodes.push(walker.currentNode);
    }
    nodes.forEach(function (node) {
      const translated = replaceText(node.nodeValue, translations);
      if (translated !== node.nodeValue) {
        node.nodeValue = translated;
      }
    });
  }

  function watchSwaggerUi(language) {
    const root = document.getElementById("swagger-ui");
    localizeSwaggerNode(root, language);
    new MutationObserver(function (mutations) {
      mutations.forEach(function (mutation) {
        mutation.addedNodes.forEach(function (node) {
          localizeSwaggerNode(node, language);
        });
      });
    }).observe(root, {childList: true, subtree: true});
  }

  function showLoadError(language, response) {
    const root = document.getElementById("swagger-ui");
    const status = response && response.status ? " (HTTP " + response.status + ")" : "";
    root.innerHTML = '<div class="docs-load-error">' + pageMessages[language].loadFailed + status + "</div>";
  }

  const language = currentLanguage();
  const specUrl = "/v3/api-docs/" + language;
  window.localStorage.setItem("fastvue.swagger.language", language);
  localizePage(language);

  document.getElementById("language-select").addEventListener("change", function (event) {
    const nextLanguage = event.target.value;
    window.localStorage.setItem("fastvue.swagger.language", nextLanguage);
    const target = new URL(window.location.href);
    target.searchParams.set("lang", nextLanguage);
    window.location.assign(target.toString());
  });

  watchSwaggerUi(language);
  window.ui = SwaggerUIBundle({
    url: specUrl,
    dom_id: "#swagger-ui",
    deepLinking: true,
    persistAuthorization: true,
    displayRequestDuration: true,
    filter: true,
    docExpansion: "none",
    operationsSorter: "method",
    tagsSorter: "alpha",
    presets: [SwaggerUIBundle.presets.apis],
    layout: "BaseLayout",
    onFailure: function (error) {
      showLoadError(language, error);
    },
    onComplete: function () {
      localizeSwaggerNode(document.getElementById("swagger-ui"), language);
    }
  });
}());
