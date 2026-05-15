# BabyLog iOS 部署说明

## 结论

BabyLog 是 PWA，不需要打包 IPA，也不走 App Store。

iPhone 上的安装方式是：

1. 把 `app/dist` 部署成一个 HTTPS 网站。
2. 用 iPhone 的 Safari 打开这个 HTTPS 地址。
3. 点 Safari 分享按钮。
4. 选择「添加到主屏幕」。
5. 从主屏幕打开后就是 installed PWA。

> 注意：必须用 Safari。微信内置浏览器、Chrome iOS 不能完成标准 PWA 安装体验。

## 当前项目已具备

| 项 | 状态 |
|---|---|
| Vite production build | 已有，运行 `npm run build` 生成 `app/dist` |
| manifest | 已有 `app/public/manifest.webmanifest` |
| service worker | 已有 `app/public/sw.js` |
| iOS web app meta | 已有 `apple-mobile-web-app-capable`、title、status bar |
| iOS 主屏幕图标 | 已补 `apple-touch-icon.png` |
| PNG PWA icons | 已补 `icon-192.png`、`icon-512.png` |

## 方案 1：临时给 iPhone 体验

适合先给家里 iPhone 看 UI。

前提：需要一个公网 HTTPS 临时地址。可选：

| 方式 | 说明 |
|---|---|
| Cloudflare Tunnel / ngrok | Windows 本地跑 Vite，通过 HTTPS tunnel 暴露给 iPhone |
| Vercel / Netlify / Cloudflare Pages | 上传或连接仓库后自动给 HTTPS |
| 腾讯云 COS 静态网站 + CDN/EdgeOne | 更接近后续正式部署 |

不建议只用 `http://电脑IP:5173`：

- iPhone 也许能打开页面，但 service worker、相机、安装态能力不稳定。
- installed PWA 最好一开始就按 HTTPS 验证。

## 方案 2：腾讯云静态部署

适合家庭长期使用。

服务器 IP、域名和 SSH 用户通过本地部署参数维护，不写入仓库；服务器部署细节见 `腾讯云部署说明.md`。

### 本地构建

```powershell
cd app
npm run build
```

构建产物在：

```text
app/dist
```

把 `dist` 目录里的内容整体上传到 HTTPS 静态站点根目录。

### 腾讯云可选承载方式

| 方式 | 推荐度 | 说明 |
|---|---:|---|
| COS 静态网站 + CDN/EdgeOne HTTPS | 高 | 最适合纯 PWA 静态文件 |
| 轻量服务器 Nginx | 中 | 你已有服务器时可用，但需要配 Nginx 和 HTTPS 证书 |
| PocketBase 后续同机托管静态文件 | 中 | 如果后端最后选 PocketBase，可以一起托管前端 |

## Nginx 示例

如果你腾讯云是 Linux 服务器，最终可以把 `dist` 上传到：

```text
/var/www/babylog
```

Nginx SPA 配置核心是：

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.example.com;

    root /var/www/babylog;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

HTTPS 证书可以用腾讯云 SSL、Let's Encrypt 或 EdgeOne。

## iPhone 安装步骤

1. iPhone 打开 Safari。
2. 输入部署后的 HTTPS 地址。
3. 确认页面能打开。
4. 点底部分享按钮。
5. 选择「添加到主屏幕」。
6. 名称填 `BabyLog`。
7. 从主屏幕图标进入。

## iOS 验证清单

| 检查项 | 期望 |
|---|---|
| 主屏幕图标 | 显示 BabyLog 图标，不是网页截图 |
| 启动方式 | 从主屏幕打开后没有 Safari 地址栏 |
| 离线 | 打开过一次后，断网仍能打开 shell |
| 相机 | 后续接表单时，`input type=file accept=image/* capture` 可调用相机 |
| 存储 | 首次建档后请求 persistent storage；不支持时提示导出 |
| 备份 | 设置页能导出 JSON 后再考虑长期家庭使用 |

## 当前最推荐路径

当前腾讯云服务器已能登录，可以先用 `腾讯云部署说明.md` 把 HTTP 预览跑起来。

iOS installed PWA 和相机能力仍建议最终走域名 + HTTPS。BabyLog 当前是纯静态 PWA，后端同步/OCR 还没接，前端部署不需要等 PocketBase/FastAPI 拍板。
