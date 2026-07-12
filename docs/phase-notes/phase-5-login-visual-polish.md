# Phase 5 Login Visual Polish

## 本次目标

基于用户提供的高级汽车/科技产品登录页参考图，实现一版更有高级感的登录/注册入口页面。

本次只改登录页视觉，不改后端接口和主业务页面。

## 设计方向

确定风格为：

```text
浅蓝灰背景 + 大卡片容器 + 左侧极简表单 + 右侧浅色科技背景图
```

明确避免：

1. 大面积渐变。
2. 真实品牌 logo。
3. 霓虹、赛博风。
4. 复杂动效库。
5. 过重毛玻璃。

## 文件改动

新增登录组件：

```text
frontend/src/LoginView.vue
```

新增背景图资源：

```text
frontend/src/assets/login-hero-bg.png
```

接入位置：

```text
frontend/src/App.vue
```

说明：

旧登录块暂时保留但设置为不渲染，新的 `LoginView` 负责登录页展示。这样如果后续风格不满意，可以较容易回退或继续对比。

## 页面结构

新登录页结构：

```text
全屏浅蓝灰背景
└── 居中大卡片
    ├── 左侧表单区 45%
    │   ├── 尖细星芒 logo
    │   ├── Get plugged in
    │   ├── 副标题
    │   ├── 两列表单
    │   ├── 黑色 Submit 按钮
    │   └── 登录错误提示
    ├── 右侧视觉区 55%
    │   ├── login-hero-bg.png 背景图
    │   └── 底部浅色操作栏
    └── 底部 footer
        ├── 分页短横线
        ├── 4 个圆形快捷按钮
        └── Demo accounts 链接
```

## 登录逻辑

沿用原有逻辑：

```text
loginForm
login()
fillDemo()
loginLoading
loginError
```

因此后端登录接口不变：

```http
POST /api/auth/login
```

演示账号仍是：

```text
admin / 123456
maintainer / 123456
user01 / 123456
```

## 响应式处理

小屏幕下：

1. 左右分栏改为上下结构。
2. 右侧图像区域缩小。
3. 更窄屏幕下隐藏右侧图像，只保留表单与 footer。
4. 两列表单改为单列。

## 验证结果

已执行：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```

结果：

```text
✓ built
```

Vite 仍提示 Element Plus / ECharts chunk 较大，这是已有体积警告，不影响当前运行。

## 后续衔接

后续继续做 Phase 5 时，可以沿用这套视觉语言改：

1. 首页仪表盘。
2. 智能排障页面。
3. 知识图谱页面。
4. 故障知识库表格页。

建议优先级：

```text
登录页 -> 首页 -> 智能排障页 -> 知识图谱页 -> 知识库表格页
```
