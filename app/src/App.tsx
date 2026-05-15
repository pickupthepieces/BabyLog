import { useState } from "react";
import "./styles.css";

type TabKey = "home" | "timeline" | "library" | "settings";

type ToneKey = "peach" | "blue" | "yellow" | "green" | "violet" | "rose";

type QuickAction = {
  label: string;
  hint: string;
  asset: string;
  tone: ToneKey;
};

const assetBase = "/assets/kindergarten-vivid";

const quickActions: QuickAction[] = [
  { label: "喂养", hint: "母乳 / 奶瓶 / 辅食", asset: "feeding-bottle.png", tone: "peach" },
  { label: "睡眠", hint: "开始 / 结束 / 地点", asset: "sleep-moon.png", tone: "blue" },
  { label: "尿布", hint: "尿 / 便 / 性状", asset: "diaper.png", tone: "yellow" },
  { label: "体温", hint: "温度 / 测量方式", asset: "thermometer.png", tone: "green" },
  { label: "用药", hint: "药名 / 剂量 / 时间", asset: "icon-pill.png", tone: "violet" },
  { label: "B超", hint: "指标 / 照片 / OCR占位", asset: "ultrasound-sheet.png", tone: "rose" }
];

const todayHighlights: Array<{ label: string; value: string; sub: string }> = [
  { label: "上次胎动", value: "1h 20m 前", sub: "2 小时内 32 次" },
  { label: "上次产检", value: "5 月 14 日", sub: "下次 2 周后" },
  { label: "孕妈体重", value: "60.4 kg", sub: "较孕前 +8.6 kg" }
];

const recentRecords: Array<{
  time: string;
  type: string;
  body: string;
  tone: ToneKey;
  attachmentCount?: number;
}> = [
  { time: "16:20", type: "胎动", body: "2 小时内 32 次", tone: "violet" },
  { time: "09:30", type: "B 超", body: "28+3 周 · EFW 1320 g · BPD 71 mm", tone: "rose", attachmentCount: 2 },
  { time: "08:00", type: "孕妈体重", body: "60.4 kg · 较孕前 +8.6 kg", tone: "green" }
];

const timelineItems: Array<{
  day?: string;
  time: string;
  type: string;
  body: string;
  tone: ToneKey;
  attachmentCount?: number;
}> = [
  { day: "今天", time: "16:20", type: "胎动", body: "2 小时内 32 次，状态稳定", tone: "violet" },
  { day: "今天", time: "09:30", type: "B 超", body: "EFW 1320 g · BPD 71 mm · HC 258 mm", tone: "rose", attachmentCount: 2 },
  { day: "昨天", time: "20:10", type: "体温", body: "36.6 °C · 腋温", tone: "green" },
  { day: "昨天", time: "08:40", type: "产检", body: "下次复查 2 周后；医生备注已拍照保存", tone: "blue", attachmentCount: 1 }
];

const trendCards = [
  { title: "胎儿 EFW", value: "1320 g", caption: "28+3 周", tone: "rose" as ToneKey, points: "4,30 26,24 48,20 70,14 92,11 116,6" },
  { title: "孕妈体重", value: "60.4 kg", caption: "较孕前 +8.6 kg", tone: "green" as ToneKey, points: "4,28 22,26 42,24 62,20 82,17 102,12 116,8" }
];

const libraryItems: Array<{ title: string; count: string; asset: string; note: string }> = [
  { title: "B 超单", count: "2 张", asset: "ultrasound-sheet.png", note: "已保存本机；OCR 待接入" },
  { title: "检查单", count: "0 张", asset: "baby-diary-notebook.png", note: "孕期常规检查、血检报告" },
  { title: "出生证明", count: "0 张", asset: "vaccine-card.png", note: "出生后启用" },
  { title: "疫苗本", count: "0 张", asset: "vaccine-card.png", note: "出生后启用" }
];

const navItems: Array<{ key: TabKey; label: string; asset: string }> = [
  { key: "home", label: "首页", asset: "baby-diary-notebook.png" },
  { key: "timeline", label: "时间线", asset: "calendar.png" },
  { key: "library", label: "资料", asset: "growth-ruler.png" },
  { key: "settings", label: "设置", asset: "icon-settings.png" }
];

function App() {
  const [activeTab, setActiveTab] = useState<TabKey>("home");
  const [isQuickSheetOpen, setQuickSheetOpen] = useState(false);

  return (
    <main className="app-shell">
      <section className="phone-frame" aria-label="BabyLog PWA 工作区">
        <AppHeader activeTab={activeTab} />
        <div className="content-scroll">
          {activeTab === "home" && <HomeView />}
          {activeTab === "timeline" && <TimelineView />}
          {activeTab === "library" && <LibraryView />}
          {activeTab === "settings" && <SettingsView />}
        </div>
        <BottomNav
          activeTab={activeTab}
          onChange={setActiveTab}
          onQuickAction={() => setQuickSheetOpen(true)}
        />
      </section>

      {isQuickSheetOpen && <QuickSheet onClose={() => setQuickSheetOpen(false)} />}
    </main>
  );
}

function AppHeader({ activeTab }: { activeTab: TabKey }) {
  const title = activeTab === "home" ? "BabyLog" : navItems.find((item) => item.key === activeTab)?.label;
  const showKicker = activeTab === "home";

  return (
    <header className="app-header">
      <div>
        {showKicker && <p className="header-kicker">单胎 / 本机模式</p>}
        <h1>{title}</h1>
      </div>
    </header>
  );
}

function HomeView() {
  return (
    <div className="view-stack">
      <section className="week-panel" aria-label="当前阶段">
        <div className="week-copy">
          <span className="stage-chip">孕期</span>
          <h2 className="num">
            孕 28<sup>+3</sup> 周
          </h2>
          <p className="num">距预产期 82 天 · 预产期 2026-08-05</p>
        </div>
        <img className="mascot-art" src={`${assetBase}/star-mascot.png`} alt="" />
      </section>

      <section className="today-panel" aria-label="今日">
        <div className="section-title">
          <h2>今日</h2>
          <span>00:00 起算</span>
        </div>
        <div className="today-grid">
          {todayHighlights.map((item) => (
            <article className="today-card" key={item.label}>
              <span className="today-label">{item.label}</span>
              <strong className="today-value num">{item.value}</strong>
              <small className="today-sub num">{item.sub}</small>
            </article>
          ))}
        </div>
      </section>

      <section aria-label="最近记录">
        <div className="section-title">
          <h2>最近记录</h2>
          <button className="text-button" type="button">
            全部记录
          </button>
        </div>
        <div className="timeline-list compact">
          {recentRecords.map((record) => (
            <TimelineRow key={`${record.time}-${record.type}`} {...record} />
          ))}
        </div>
      </section>

      <section aria-label="趋势">
        <div className="section-title">
          <h2>趋势</h2>
          <span>点击查看曲线</span>
        </div>
        <div className="trend-grid">
          {trendCards.map((card) => (
            <TrendCard key={card.title} {...card} />
          ))}
        </div>
      </section>
    </div>
  );
}

function TimelineView() {
  return (
    <div className="view-stack">
      <section className="filter-panel" aria-label="时间线筛选">
        <div className="chip-row">
          {["全部", "孕期", "育儿", "B 超", "体温", "产检"].map((label, index) => (
            <button className={index === 0 ? "chip active" : "chip"} type="button" key={label}>
              {label}
            </button>
          ))}
        </div>
      </section>

      <section className="disclaimer" aria-label="医疗免责声明">
        曲线和参考提示只用于家庭记录，不能替代医生判断。
      </section>

      <section className="timeline-list" aria-label="时间线记录">
        {timelineItems.map((item) => (
          <TimelineRow key={`${item.day}-${item.time}-${item.type}`} {...item} />
        ))}
      </section>
    </div>
  );
}

function LibraryView() {
  return (
    <div className="view-stack">
      <section className="library-grid" aria-label="资料分类">
        {libraryItems.map((item) => (
          <article className="library-item" key={item.title}>
            <img src={`${assetBase}/${item.asset}`} alt="" />
            <div>
              <div className="library-line">
                <h3>{item.title}</h3>
                <span className="num">{item.count}</span>
              </div>
              <p>{item.note}</p>
            </div>
          </article>
        ))}
      </section>

      <section className="disclaimer pinned" aria-label="曲线免责声明">
        FGR / 成长曲线标准数据尚未落库；当前曲线只显示自有趋势。点击首页趋势卡进入全屏曲线。
      </section>
    </div>
  );
}

function SettingsView() {
  return (
    <div className="view-stack">
      <section className="settings-panel" aria-label="档案">
        <h2>档案</h2>
        <SettingRow label="当前范围" value="单胎 / 单宝宝" />
        <SettingRow label="预产期" value="2026-08-05" />
        <SettingRow label="日界" value="自然日 00:00" interactive />
        <SettingRow label="夜间柔光" value="跟随系统" interactive />
      </section>

      <section className="settings-panel" aria-label="数据">
        <h2>数据</h2>
        <BackupRow />
        <QuotaRow />
        <SettingRow label="从备份导入" value="JSON" interactive />
        <SettingRow label="清空本地数据" value="" interactive destructive />
      </section>

      <section className="settings-panel" aria-label="同步">
        <h2>同步</h2>
        <SettingRow label="后端" value="后端未配置" />
        <SettingRow label="服务端地域" value="—" />
        <SettingRow label="最近健康检查" value="—" />
        <button className="primary-button outline" type="button">
          配置同步
        </button>
        <p className="settings-hint">
          启用同步会将胎儿、孕期、育儿记录上传到你选定的服务器（可能位于境外）；启用前需要单独勾选确认。
        </p>
      </section>

      <section className="settings-panel" aria-label="关于">
        <h2>关于</h2>
        <SettingRow label="版本" value="0.1.0 · MVP" />
        <SettingRow label="医疗免责声明" value="" interactive />
        <SettingRow label="隐私说明" value="" interactive />
      </section>
    </div>
  );
}

function BackupRow() {
  return (
    <div className="backup-row">
      <div>
        <strong>本地备份</strong>
        <span className="num">距上次导出 0 天</span>
      </div>
      <button className="primary-button" type="button">
        导出
      </button>
    </div>
  );
}

function QuotaRow() {
  return (
    <div className="quota-row" aria-label="存储用量">
      <div className="quota-line">
        <span>本机用量</span>
        <strong className="num">5.0 MB / 10.0 MB</strong>
      </div>
      <div className="quota-bar">
        <span style={{ width: "50%" }} />
      </div>
    </div>
  );
}

function BottomNav({
  activeTab,
  onChange,
  onQuickAction
}: {
  activeTab: TabKey;
  onChange: (key: TabKey) => void;
  onQuickAction: () => void;
}) {
  return (
    <nav className="bottom-nav" aria-label="主导航">
      <div className="nav-pair">
        {navItems.slice(0, 2).map((item) => (
          <NavButton key={item.key} item={item} active={activeTab === item.key} onClick={() => onChange(item.key)} />
        ))}
      </div>
      <button className="fab" type="button" aria-label="快捷记录" onClick={onQuickAction}>
        <span>+</span>
      </button>
      <div className="nav-pair">
        {navItems.slice(2).map((item) => (
          <NavButton key={item.key} item={item} active={activeTab === item.key} onClick={() => onChange(item.key)} />
        ))}
      </div>
    </nav>
  );
}

function NavButton({
  item,
  active,
  onClick
}: {
  item: (typeof navItems)[number];
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button className={active ? "nav-item active" : "nav-item"} type="button" onClick={onClick}>
      <img src={`${assetBase}/${item.asset}`} alt="" />
      <span>{item.label}</span>
    </button>
  );
}

function QuickSheet({ onClose }: { onClose: () => void }) {
  return (
    <div className="sheet-layer">
      <button className="sheet-scrim" type="button" aria-label="关闭快捷记录" onClick={onClose} />
      <section className="quick-sheet" role="dialog" aria-label="快捷记录">
        <div className="sheet-handle" />
        <div className="section-title">
          <h2>快捷记录</h2>
          <button className="text-button" type="button" onClick={onClose}>
            关闭
          </button>
        </div>
        <div className="sheet-grid">
          {quickActions.map((action) => (
            <button className={`sheet-action tone-${action.tone}`} type="button" key={action.label}>
              <img src={`${assetBase}/${action.asset}`} alt="" />
              <span>{action.label}</span>
              <small>{action.hint}</small>
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}

function TimelineRow({
  day,
  time,
  type,
  body,
  tone,
  attachmentCount
}: {
  day?: string;
  time: string;
  type: string;
  body: string;
  tone: ToneKey;
  attachmentCount?: number;
}) {
  return (
    <article className={`timeline-row tone-${tone}`}>
      <span className="timeline-dot" />
      <div className="timeline-body">
        {day && <span className="timeline-day">{day}</span>}
        <div className="timeline-main">
          <span className="num timeline-time">{time}</span>
          <strong>{type}</strong>
          {attachmentCount ? (
            <span className="tag attachment-tag" aria-label={`含 ${attachmentCount} 张附件`}>
              📎 {attachmentCount}
            </span>
          ) : null}
        </div>
        <p className="num">{body}</p>
      </div>
    </article>
  );
}

function TrendCard({
  title,
  value,
  caption,
  tone,
  points
}: {
  title: string;
  value: string;
  caption: string;
  tone: ToneKey;
  points: string;
}) {
  return (
    <article className={`trend-card tone-${tone}`}>
      <span>{title}</span>
      <strong className="num">{value}</strong>
      <small>{caption}</small>
      <svg viewBox="0 0 120 36" aria-hidden="true">
        <polyline points={points} />
      </svg>
    </article>
  );
}

function SettingRow({
  label,
  value,
  interactive,
  destructive
}: {
  label: string;
  value: string;
  interactive?: boolean;
  destructive?: boolean;
}) {
  const className = [
    "setting-row",
    interactive ? "interactive" : "",
    destructive ? "destructive" : ""
  ]
    .filter(Boolean)
    .join(" ");
  return (
    <div className={className}>
      <span>{label}</span>
      <strong>{value}</strong>
      {interactive && <em aria-hidden="true">›</em>}
    </div>
  );
}

export default App;
