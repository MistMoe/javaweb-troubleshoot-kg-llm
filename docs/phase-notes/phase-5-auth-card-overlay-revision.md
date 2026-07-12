# Phase 5 Auth Card Overlay Revision

## 本次目标

修正上一版登录页的问题：

1. 背景图不再只属于右侧区域。
2. `auth-card` 整体铺满背景图。
3. 四个分区作为 overlay 面板覆盖在同一张背景图上。
4. 去掉卡片和分区圆角。
5. 注册入口从左下 footer 移到 Submit 右侧。

## 改动文件

```text
frontend/src/LoginView.vue
frontend/src/RegisterView.vue
frontend/src/ThinStarLogo.vue
frontend/src/auth-view.css
frontend/src/App.vue
frontend/src/assets/login-hero-bg.png
```

背景图已替换为用户提供的新图：

```text
C:/Users/97780/Desktop/1.png
```

项目内路径：

```text
frontend/src/assets/login-hero-bg.png
```

## 布局结构

`auth-card` 使用四块 CSS Grid：

```text
grid-template-columns: 46% 54%;
grid-template-rows: minmax(0, 1fr) 96px;
grid-template-areas:
  "form hero"
  "footerLeft footerRight";
```

四区职责：

```text
form-panel: 左上表单区，浅色毛玻璃
hero-panel: 右上透明区，直接显示 auth-card 背景图
footer-left: 左下近白底栏，只放分页短横线和社交圆按钮
footer-right: 右下浅色毛玻璃底栏，显示低对比说明文字
```

## 材质调整

`auth-card`：

```text
background-image: url("./assets/login-hero-bg.png")
background-size: cover
background-position: center right
background-repeat: no-repeat
border-radius: 0
```

`form-panel`：

```text
rgba(238, 244, 249, 0.78)
blur(20px)
```

`footer-left`：

```text
rgba(250, 252, 254, 0.92)
no backdrop-filter
```

`hero-panel`：

```text
transparent
```

`footer-right`：

```text
rgba(238, 244, 249, 0.58)
blur(18px)
```

## 注册页

新增：

```text
frontend/src/RegisterView.vue
```

当前注册页是视觉页，不接后端注册接口。点击 `Back to sign in` 返回登录页。

`App.vue` 新增 `authMode` 状态：

```text
login
register
```

业务登录逻辑没有改动。

## 按钮调整

登录页表单下方按钮组：

```text
[ Submit ] [ Create account ]
```

`Submit` 是黑色主按钮。

`Create account` 是白色弱按钮，不使用蓝色、黑色、渐变或发光。

左下 footer 不再放注册按钮。

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

说明：

1. 首次构建因大图和现有 ECharts/Element Plus bundle 打包较慢，短超时命令中断。
2. 延长超时时间后构建成功。
3. 仍有既有 chunk 体积警告，不影响运行。
