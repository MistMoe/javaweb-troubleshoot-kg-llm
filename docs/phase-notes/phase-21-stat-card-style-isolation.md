# Phase 21：统计卡片布局隔离修复

## 本步目标

修复“故障知识库”和“我的收藏”页面中，头部右侧统计卡片被挤到下一行并变成大卡片的问题。

## 问题原因

多个页面都使用了同名 `.stat-card` 样式。由于 Vue 页面样式并非全部 scoped，后加载的个人资料页样式会覆盖知识库页和收藏页的统计卡片布局，导致原本应该位于头部卡片右侧的三块小统计卡被拉宽、换行。

## 本步修改

- `frontend/src/views/knowledge/knowledge-base-view.css`
  - 将 `.stat-card` 相关规则限定为 `.knowledge-grid > .stat-card`。
  - 保持知识库头部右侧三块统计卡为窄卡片。

- `frontend/src/views/favorites/favorite-cases-view.css`
  - 将 `.stat-card` 相关规则限定为 `.favorites-grid > .stat-card`。
  - 保持收藏页头部右侧摘要卡片位置稳定。

- `frontend/src/views/profile/profile-view.css`
  - 将个人资料页 `.stat-card` 相关规则限定为 `.profile-grid > .stat-card`。
  - 避免个人资料页样式污染其他业务页面。

## 后续衔接

以后新增页面时，如果使用通用类名，例如 `.stat-card`、`.detail-card`、`.list-card`，建议加上页面容器前缀，避免跨页面样式互相覆盖。
