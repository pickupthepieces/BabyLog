# BabyLog Vultr 部署说明

## 推荐机器

| 项 | 建议 |
|---|---|
| 系统 | Ubuntu 24.04 LTS 或 Ubuntu 22.04 LTS |
| 规格 | 1 vCPU / 1 GB RAM 起步即可 |
| 地区 | 优先选家里访问延迟低的节点，例如 Tokyo、Singapore、Seoul、Los Angeles |
| 登录 | root 密码或 SSH key 均可 |

BabyLog 当前是纯静态 PWA，Vultr 只需要跑 Nginx，不需要先装 Node.js。

## 连接服务器

在 Windows PowerShell：

```powershell
ssh root@<VULTR_IP>
```

如果 Vultr 创建实例时绑定的是 SSH key：

```powershell
ssh -i C:\path\to\your-key.pem root@<VULTR_IP>
```

首次连接输入 `yes`。密码不要写进聊天或文档。

## Vultr 防火墙

如果实例绑定了 Vultr Firewall，入站规则至少放行：

| 协议 | 端口 | 来源 |
|---|---:|---|
| TCP | `22` | 你的当前公网 IP，临时也可 `0.0.0.0/0` |
| TCP | `80` | `0.0.0.0/0` |
| TCP | `443` | `0.0.0.0/0` |

## 服务器初始化

SSH 登录后执行：

```bash
apt update
apt install -y nginx
systemctl enable --now nginx
systemctl status nginx --no-pager
```

如果系统启用了 UFW：

```bash
ufw allow OpenSSH
ufw allow 'Nginx Full'
ufw status
```

浏览器打开：

```text
http://<VULTR_IP>
```

能看到 Nginx 欢迎页就说明 HTTP 通了。

## 安装 BabyLog Nginx 配置

在 Windows 项目目录执行：

```powershell
scp .\deploy\nginx-babylog.conf root@<VULTR_IP>:/tmp/babylog.conf
ssh root@<VULTR_IP> "cp /tmp/babylog.conf /etc/nginx/conf.d/babylog.conf && nginx -t && systemctl reload nginx"
```

## 部署 PWA

在 Windows 项目目录执行：

```powershell
.\deploy\deploy-pwa.ps1 -HostName <VULTR_IP> -User root
```

部署完成后访问：

```text
http://<VULTR_IP>
```

## HTTPS 和 iPhone 安装

iOS installed PWA、Service Worker、后续相机能力都建议使用 HTTPS。

1. 准备域名。
2. 给域名添加 A 记录指向 Vultr 实例公网 IP。
3. 等 DNS 生效后，在服务器执行：

```bash
apt install -y certbot python3-certbot-nginx
certbot --nginx -d your-domain.example
```

4. iPhone Safari 打开 `https://your-domain.example`。
5. 分享按钮 -> 添加到主屏幕。

## 常见问题

| 现象 | 处理 |
|---|---|
| SSH timeout | 检查 Vultr Firewall、实例状态、本机网络 |
| `Permission denied` | 用户名、密码或 SSH key 不对；Vultr Ubuntu 默认通常可用 `root` |
| HTTP 打不开 | 检查 Nginx 是否运行、Vultr Firewall 是否放行 `80` |
| iPhone 不能安装成 PWA | 用 Safari 打开 HTTPS 域名，不要用微信内置浏览器 |
| 相机能力不稳定 | 先上 HTTPS；HTTP 裸 IP 只适合早期预览 |
