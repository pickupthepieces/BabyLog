# BabyLog

BabyLog 是一个家庭自用的孕育记录 PWA，目标覆盖怀孕全阶段到宝宝 3 岁。当前优先做单机版：iOS Safari / installed PWA 先验证，Android Chrome 紧跟回归；后端、同步和 OCR 都先保留接口，不阻塞本地使用。

## 当前范围

- PWA，不做原生 App。
- 当前 App 仅支持单胎/单宝宝 UI；数据模型保留 `familyId` / `childId`，为后续扩展预留。
- 本地优先，IndexedDB 保存数据。
- 后端技术栈未决，倾向 PocketBase，FastAPI 仍为候选。
- 医疗内容只做记录、趋势和参考提示，不做诊断。

## 项目结构

| 路径 | 说明 |
|---|---|
| `app/` | Vite + React + TypeScript PWA |
| `app/src/domain/` | 数据模型、孕周/年龄计算、B 超字段定义 |
| `app/src/storage/` | IndexedDB repository、备份、附件、同步队列 |
| `app/src/adapters/` | 后端、OCR、浏览器存储适配层 |
| `app/src/services/` | UI 可复用的应用服务层 |
| `deploy/` | Nginx 配置和 Windows 部署脚本 |
| `prototype/` | 静态 HTML 原型 |
| `*.md` | 立项、需求、UI 和部署文档 |

## 本地开发

```powershell
cd app
npm install
npm run dev
```

## 验证

```powershell
cd app
npm test
npm run build
```

当前最近一次验证：`9 files / 40 tests passed`，`npm run build` passed。

## 部署

腾讯云服务器部署说明见：

- `腾讯云部署说明.md`
- `iOS部署说明.md`
- `deploy/nginx-babylog.conf`
- `deploy/deploy-pwa.ps1`

服务器 IP、域名、SSH 用户等部署参数不写入仓库。部署时通过脚本参数传入。
