import { useCallback, useEffect, useMemo, useState } from "react";
import { estimateStorageUsage } from "./adapters/storage";
import type { BabyLogEvent, CreateEventInput, EventType } from "./domain/types";
import { createBackup } from "./storage/backup";
import { formatStorageEstimate } from "./storage/attachments";
import { createLocalRepository } from "./storage/localRepository";
import { listPendingSyncChanges } from "./storage/syncQueue";
import { listRecentEvents, recordLocalEvent, recordLocalUltrasound, summarizeEventDay } from "./services/logService";
import type { LocalUltrasoundFields } from "./services/logService";
import type { EventDaySummary } from "./services/logService";
import "./styles.css";

type TabKey = "home" | "timeline" | "library" | "settings";

type ToneKey = "peach" | "blue" | "yellow" | "green" | "violet" | "rose";

type QuickAction = {
  label: string;
  hint: string;
  asset: string;
  tone: ToneKey;
  eventType: EventType;
  summary: string;
};

type TimelineItem = {
  id?: string;
  day?: string;
  time: string;
  type: string;
  body: string;
  tone: ToneKey;
  attachmentCount?: number;
};

type DashboardState = {
  recentEvents: BabyLogEvent[];
  summary: EventDaySummary | null;
  pendingSyncCount: number;
  storageUsage: string;
  isLoading: boolean;
};

type UltrasoundFormState = {
  examDate: string;
  gestationalAge: string;
  bpdMm: string;
  hcMm: string;
  acMm: string;
  flMm: string;
  efwGram: string;
  imageFile?: File;
};

const assetBase = "/assets/kindergarten-vivid";
const localDbName = "babylog-local";
const localFamilyId = "family_local";
const localChildId = "child_singleton";

const quickActions: QuickAction[] = [
  {
    label: "喂养",
    hint: "母乳 / 奶瓶 / 辅食",
    asset: "feeding-bottle.png",
    tone: "peach",
    eventType: "feed",
    summary: "快捷记录 · 待补充奶量/方式"
  },
  {
    label: "睡眠",
    hint: "开始 / 结束 / 地点",
    asset: "sleep-moon.png",
    tone: "blue",
    eventType: "sleep",
    summary: "快捷记录 · 待补充睡眠时长"
  },
  {
    label: "尿布",
    hint: "尿 / 便 / 性状",
    asset: "diaper.png",
    tone: "yellow",
    eventType: "diaper",
    summary: "快捷记录 · 待补充尿/便细节"
  },
  {
    label: "体温",
    hint: "温度 / 测量方式",
    asset: "thermometer.png",
    tone: "green",
    eventType: "temperature",
    summary: "快捷记录 · 待补充温度数值"
  },
  {
    label: "用药",
    hint: "药名 / 剂量 / 时间",
    asset: "icon-pill.png",
    tone: "violet",
    eventType: "medication",
    summary: "快捷记录 · 待补充药名/剂量"
  },
  {
    label: "B超",
    hint: "指标 / 照片 / OCR占位",
    asset: "ultrasound-sheet.png",
    tone: "rose",
    eventType: "ultrasound",
    summary: "B 超快捷记录 · 待补指标/照片"
  }
];

const trendCards = [
  { title: "胎儿 EFW", value: "1320 g", caption: "28+3 周", tone: "rose" as ToneKey, points: "4,30 26,24 48,20 70,14 92,11 116,6" },
  { title: "孕妈体重", value: "60.4 kg", caption: "较孕前 +8.6 kg", tone: "green" as ToneKey, points: "4,28 22,26 42,24 62,20 82,17 102,12 116,8" }
];

const libraryItems: Array<{ title: string; count: string; asset: string; note: string }> = [
  { title: "B 超单", count: "0 张", asset: "ultrasound-sheet.png", note: "已保存本机；OCR 待接入" },
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

const eventLabels: Record<EventType, string> = {
  pregnancy_checkup: "产检",
  ultrasound: "B 超",
  fetal_movement: "胎动",
  contraction: "宫缩",
  birth: "出生",
  feed: "喂养",
  sleep: "睡眠",
  diaper: "尿布",
  temperature: "体温",
  medication: "用药",
  illness: "不适",
  growth: "成长",
  vaccine: "疫苗",
  milestone: "里程碑",
  note: "备注"
};

const eventTones: Record<EventType, ToneKey> = {
  pregnancy_checkup: "blue",
  ultrasound: "rose",
  fetal_movement: "violet",
  contraction: "rose",
  birth: "peach",
  feed: "peach",
  sleep: "blue",
  diaper: "yellow",
  temperature: "green",
  medication: "violet",
  illness: "rose",
  growth: "green",
  vaccine: "yellow",
  milestone: "violet",
  note: "blue"
};

function App() {
  const repository = useMemo(() => createLocalRepository(localDbName), []);
  const [activeTab, setActiveTab] = useState<TabKey>("home");
  const [isQuickSheetOpen, setQuickSheetOpen] = useState(false);
  const [isUltrasoundFormOpen, setUltrasoundFormOpen] = useState(false);
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [isSavingUltrasound, setSavingUltrasound] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [ultrasoundForm, setUltrasoundForm] = useState<UltrasoundFormState>(() => createDefaultUltrasoundForm());
  const [refreshVersion, setRefreshVersion] = useState(0);
  const [dashboard, setDashboard] = useState<DashboardState>({
    recentEvents: [],
    summary: null,
    pendingSyncCount: 0,
    storageUsage: "读取中",
    isLoading: true
  });

  const refreshLocalData = useCallback(() => {
    setRefreshVersion((version) => version + 1);
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function loadLocalData() {
      const { startIso, endIso } = getLocalDayRange(new Date());
      const [recentEvents, summary, pendingChanges, storageEstimate] = await Promise.all([
        listRecentEvents(repository, localFamilyId, 20),
        summarizeEventDay(repository, localFamilyId, startIso, endIso),
        listPendingSyncChanges(repository, localFamilyId),
        estimateStorageUsage()
      ]);

      if (!cancelled) {
        setDashboard({
          recentEvents,
          summary,
          pendingSyncCount: pendingChanges.length,
          storageUsage: formatStorageEstimateForUi(storageEstimate),
          isLoading: false
        });
      }
    }

    loadLocalData().catch((error) => {
      if (!cancelled) {
        setDashboard((current) => ({ ...current, isLoading: false }));
        setToast(`本地数据读取失败：${getErrorMessage(error)}`);
      }
    });

    return () => {
      cancelled = true;
    };
  }, [repository, refreshVersion]);

  async function handleQuickRecord(action: QuickAction) {
    if (action.eventType === "ultrasound") {
      setQuickSheetOpen(false);
      setUltrasoundForm(createDefaultUltrasoundForm());
      setUltrasoundFormOpen(true);
      return;
    }

    setBusyAction(action.label);
    try {
      await recordLocalEvent(repository, createQuickEventInput(action));
      setQuickSheetOpen(false);
      setToast(`${action.label}已保存到本机，等待同步`);
      refreshLocalData();
    } catch (error) {
      setToast(`保存失败：${getErrorMessage(error)}`);
    } finally {
      setBusyAction(null);
    }
  }

  async function handleSaveUltrasound() {
    setSavingUltrasound(true);
    try {
      await recordLocalUltrasound(repository, {
        familyId: localFamilyId,
        childId: localChildId,
        occurredAt: createOccurredAtFromDate(ultrasoundForm.examDate),
        fields: buildUltrasoundFields(ultrasoundForm),
        imageFile: ultrasoundForm.imageFile
      });
      setUltrasoundFormOpen(false);
      setToast("B 超已保存到本机，等待同步");
      refreshLocalData();
    } catch (error) {
      setToast(`B 超保存失败：${getErrorMessage(error)}`);
    } finally {
      setSavingUltrasound(false);
    }
  }

  async function handleExportBackup() {
    try {
      const [familyProfiles, childProfiles, events, attachments, syncChanges] = await Promise.all([
        repository.listByFamily("familyProfiles", localFamilyId),
        repository.listByFamily("childProfiles", localFamilyId),
        repository.listByFamily("events", localFamilyId),
        repository.listByFamily("attachments", localFamilyId),
        repository.listByFamily("syncChanges", localFamilyId)
      ]);
      const backup = createBackup({ familyProfiles, childProfiles, events, attachments, syncChanges });

      downloadJson(`babylog-backup-${formatDateForFilename(new Date())}.json`, backup);
      setToast(`备份已生成：${events.length} 条记录`);
    } catch (error) {
      setToast(`备份失败：${getErrorMessage(error)}`);
    }
  }

  return (
    <main className="app-shell">
      <section className="phone-frame" aria-label="BabyLog PWA 工作区">
        <AppHeader activeTab={activeTab} />
        <div className="content-scroll">
          {toast && <div className="toast" role="status">{toast}</div>}
          {activeTab === "home" && (
            <HomeView
              dashboard={dashboard}
              onShowTimeline={() => setActiveTab("timeline")}
            />
          )}
          {activeTab === "timeline" && <TimelineView events={dashboard.recentEvents} />}
          {activeTab === "library" && <LibraryView />}
          {activeTab === "settings" && (
            <SettingsView
              pendingSyncCount={dashboard.pendingSyncCount}
              storageUsage={dashboard.storageUsage}
              onExportBackup={handleExportBackup}
            />
          )}
        </div>
        <BottomNav
          activeTab={activeTab}
          onChange={setActiveTab}
          onQuickAction={() => setQuickSheetOpen(true)}
        />
      </section>

      {isQuickSheetOpen && (
        <QuickSheet
          busyAction={busyAction}
          onClose={() => setQuickSheetOpen(false)}
          onRecord={handleQuickRecord}
        />
      )}

      {isUltrasoundFormOpen && (
        <UltrasoundFormSheet
          form={ultrasoundForm}
          isSaving={isSavingUltrasound}
          onChange={(patch) => setUltrasoundForm((current) => ({ ...current, ...patch }))}
          onClose={() => setUltrasoundFormOpen(false)}
          onSave={handleSaveUltrasound}
        />
      )}
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

function HomeView({
  dashboard,
  onShowTimeline
}: {
  dashboard: DashboardState;
  onShowTimeline: () => void;
}) {
  const highlights = buildTodayHighlights(dashboard);
  const recentRows = dashboard.recentEvents.slice(0, 3).map(eventToTimelineItem);

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
          {highlights.map((item) => (
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
          <button className="text-button" type="button" onClick={onShowTimeline}>
            全部记录
          </button>
        </div>
        <div className="timeline-list compact">
          {recentRows.length > 0 ? (
            recentRows.map((record) => <TimelineRow key={record.id} {...record} />)
          ) : (
            <EmptyState text={dashboard.isLoading ? "正在读取本机记录" : "暂无本地记录，点 + 开始记录。"} />
          )}
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

function TimelineView({ events }: { events: BabyLogEvent[] }) {
  const rows = events.map(eventToTimelineItem);

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
        {rows.length > 0 ? (
          rows.map((item) => <TimelineRow key={item.id} {...item} />)
        ) : (
          <EmptyState text="暂无本地记录。快捷记录会先保存到本机，再等待同步上传。" />
        )}
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

function SettingsView({
  pendingSyncCount,
  storageUsage,
  onExportBackup
}: {
  pendingSyncCount: number;
  storageUsage: string;
  onExportBackup: () => void;
}) {
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
        <BackupRow onExportBackup={onExportBackup} />
        <QuotaRow storageUsage={storageUsage} />
        <SettingRow label="从备份导入" value="JSON" interactive />
        <SettingRow label="清空本地数据" value="" interactive destructive />
      </section>

      <section className="settings-panel" aria-label="同步">
        <h2>同步</h2>
        <SettingRow label="后端" value="后端未配置" />
        <SettingRow label="待同步记录" value={`${pendingSyncCount} 条待上传`} />
        <SettingRow label="服务端地域" value="—" />
        <SettingRow label="最近健康检查" value="—" />
        <button className="primary-button outline" type="button">
          配置同步
        </button>
        <p className="settings-hint">
          当前所有记录先保存到本机 IndexedDB；启用同步后，再把 pending 队列上传到你选定的服务器。
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

function BackupRow({ onExportBackup }: { onExportBackup: () => void }) {
  return (
    <div className="backup-row">
      <div>
        <strong>本地备份</strong>
        <span className="num">导出 JSON，附件二进制后续单独打包</span>
      </div>
      <button className="primary-button" type="button" onClick={onExportBackup}>
        导出
      </button>
    </div>
  );
}

function QuotaRow({ storageUsage }: { storageUsage: string }) {
  return (
    <div className="quota-row" aria-label="存储用量">
      <div className="quota-line">
        <span>本机用量</span>
        <strong className="num">{storageUsage}</strong>
      </div>
      <div className="quota-bar">
        <span style={{ width: storageUsage.includes("(") ? storageUsage.match(/\((\d+)%\)/)?.[1] + "%" : "4%" }} />
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

function QuickSheet({
  busyAction,
  onClose,
  onRecord
}: {
  busyAction: string | null;
  onClose: () => void;
  onRecord: (action: QuickAction) => void;
}) {
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
            <button
              className={`sheet-action tone-${action.tone}`}
              type="button"
              key={action.label}
              disabled={busyAction !== null}
              onClick={() => onRecord(action)}
            >
              <img src={`${assetBase}/${action.asset}`} alt="" />
              <span>{busyAction === action.label ? "保存中" : action.label}</span>
              <small>{action.hint}</small>
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}

function UltrasoundFormSheet({
  form,
  isSaving,
  onChange,
  onClose,
  onSave
}: {
  form: UltrasoundFormState;
  isSaving: boolean;
  onChange: (patch: Partial<UltrasoundFormState>) => void;
  onClose: () => void;
  onSave: () => void;
}) {
  return (
    <div className="sheet-layer">
      <button className="sheet-scrim" type="button" aria-label="关闭 B 超记录" onClick={onClose} />
      <section className="quick-sheet form-sheet" role="dialog" aria-label="B 超记录">
        <div className="sheet-handle" />
        <div className="section-title">
          <h2>B 超记录</h2>
          <button className="text-button" type="button" onClick={onClose}>
            关闭
          </button>
        </div>
        <div className="form-grid">
          <label>
            <span>检查日期</span>
            <input
              type="date"
              value={form.examDate}
              onChange={(event) => onChange({ examDate: event.currentTarget.value })}
            />
          </label>
          <label>
            <span>孕周</span>
            <input
              inputMode="text"
              placeholder="28+3"
              value={form.gestationalAge}
              onChange={(event) => onChange({ gestationalAge: event.currentTarget.value })}
            />
          </label>
          <label>
            <span>双顶径 BPD</span>
            <input
              inputMode="decimal"
              placeholder="mm"
              value={form.bpdMm}
              onChange={(event) => onChange({ bpdMm: event.currentTarget.value })}
            />
          </label>
          <label>
            <span>头围 HC</span>
            <input
              inputMode="decimal"
              placeholder="mm"
              value={form.hcMm}
              onChange={(event) => onChange({ hcMm: event.currentTarget.value })}
            />
          </label>
          <label>
            <span>腹围 AC</span>
            <input
              inputMode="decimal"
              placeholder="mm"
              value={form.acMm}
              onChange={(event) => onChange({ acMm: event.currentTarget.value })}
            />
          </label>
          <label>
            <span>股骨长 FL</span>
            <input
              inputMode="decimal"
              placeholder="mm"
              value={form.flMm}
              onChange={(event) => onChange({ flMm: event.currentTarget.value })}
            />
          </label>
          <label>
            <span>估计胎重 EFW</span>
            <input
              inputMode="decimal"
              placeholder="g"
              value={form.efwGram}
              onChange={(event) => onChange({ efwGram: event.currentTarget.value })}
            />
          </label>
          <label className="file-field">
            <span>B 超单照片</span>
            <input
              type="file"
              aria-label="B 超单照片"
              accept="image/*"
              capture="environment"
              onChange={(event) => onChange({ imageFile: event.currentTarget.files?.[0] })}
            />
            <small>{form.imageFile ? form.imageFile.name : "可直接拍照或选择相册图片"}</small>
          </label>
        </div>
        <button className="primary-button full-width" type="button" disabled={isSaving} onClick={onSave}>
          {isSaving ? "保存中" : "保存 B 超记录"}
        </button>
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
}: TimelineItem) {
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
              附件 {attachmentCount}
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

function EmptyState({ text }: { text: string }) {
  return <p className="empty-state">{text}</p>;
}

function createQuickEventInput(action: QuickAction): CreateEventInput {
  return {
    familyId: localFamilyId,
    childId: localChildId,
    eventType: action.eventType,
    occurredAt: new Date().toISOString(),
    payload: {
      summary: action.summary,
      quickAction: action.label
    }
  };
}

function createDefaultUltrasoundForm(): UltrasoundFormState {
  return {
    examDate: formatDateInputValue(new Date()),
    gestationalAge: "28+3",
    bpdMm: "",
    hcMm: "",
    acMm: "",
    flMm: "",
    efwGram: ""
  };
}

function buildUltrasoundFields(form: UltrasoundFormState): LocalUltrasoundFields {
  return {
    examDate: form.examDate,
    gestationalAgeDays: parseGestationalAgeDays(form.gestationalAge),
    bpdMm: parseOptionalNumber(form.bpdMm),
    hcMm: parseOptionalNumber(form.hcMm),
    acMm: parseOptionalNumber(form.acMm),
    flMm: parseOptionalNumber(form.flMm),
    efwGram: parseOptionalNumber(form.efwGram)
  };
}

function parseGestationalAgeDays(value: string): number | undefined {
  const match = value.trim().match(/^(\d{1,2})(?:\s*\+\s*(\d))?/);
  if (!match) {
    return undefined;
  }

  return Number(match[1]) * 7 + Number(match[2] ?? 0);
}

function parseOptionalNumber(value: string): number | undefined {
  const normalized = value.trim();
  if (normalized === "") {
    return undefined;
  }

  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function createOccurredAtFromDate(date: string): string {
  if (!date) {
    return new Date().toISOString();
  }

  const now = new Date();
  const hour = String(now.getHours()).padStart(2, "0");
  const minute = String(now.getMinutes()).padStart(2, "0");
  const second = String(now.getSeconds()).padStart(2, "0");

  return new Date(`${date}T${hour}:${minute}:${second}`).toISOString();
}

function formatDateInputValue(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function buildTodayHighlights(dashboard: DashboardState): Array<{ label: string; value: string; sub: string }> {
  const total = dashboard.summary ? Object.values(dashboard.summary.counts).reduce((sum, count) => sum + count, 0) : 0;
  const latest = dashboard.recentEvents[0];

  return [
    { label: "今日记录", value: `${total} 条`, sub: "已保存到本机" },
    {
      label: "上次记录",
      value: latest ? formatRelativeTime(latest.occurredAt) : "暂无",
      sub: latest ? eventLabels[latest.eventType] : "点 + 开始"
    },
    {
      label: "待同步",
      value: dashboard.pendingSyncCount > 0 ? `待同步 ${dashboard.pendingSyncCount} 条` : "0 条",
      sub: "服务器未配置"
    }
  ];
}

function eventToTimelineItem(event: BabyLogEvent): TimelineItem {
  return {
    id: event.id,
    day: formatEventDay(event.occurredAt),
    time: formatEventTime(event.occurredAt),
    type: eventLabels[event.eventType],
    body: getEventSummary(event),
    tone: eventTones[event.eventType],
    attachmentCount: event.attachmentIds.length || undefined
  };
}

function getEventSummary(event: BabyLogEvent): string {
  if (typeof event.payload.summary === "string") {
    return event.payload.summary;
  }

  return "手动记录 · 待补充详情";
}

function getLocalDayRange(date: Date): { startIso: string; endIso: string } {
  const start = new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0, 0);
  const end = new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59, 999);

  return { startIso: start.toISOString(), endIso: end.toISOString() };
}

function formatEventTime(iso: string): string {
  return new Intl.DateTimeFormat("zh-CN", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false
  }).format(new Date(iso));
}

function formatEventDay(iso: string): string {
  const event = new Date(iso);
  const now = new Date();
  const eventStart = new Date(event.getFullYear(), event.getMonth(), event.getDate()).getTime();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const dayDiff = Math.round((todayStart - eventStart) / 86_400_000);

  if (dayDiff === 0) {
    return "今天";
  }
  if (dayDiff === 1) {
    return "昨天";
  }

  return new Intl.DateTimeFormat("zh-CN", { month: "numeric", day: "numeric" }).format(event);
}

function formatRelativeTime(iso: string): string {
  const diffMs = Math.max(0, Date.now() - Date.parse(iso));
  const diffMinutes = Math.floor(diffMs / 60_000);

  if (diffMinutes < 1) {
    return "刚刚";
  }
  if (diffMinutes < 60) {
    return `${diffMinutes}m 前`;
  }

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) {
    return `${diffHours}h 前`;
  }

  return formatEventDay(iso);
}

function formatStorageEstimateForUi(estimate: StorageEstimate | null): string {
  const formatted = formatStorageEstimate(estimate);

  return formatted === "Storage estimate unavailable" ? "浏览器未提供估算" : formatted;
}

function downloadJson(filename: string, data: unknown) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");

  anchor.href = url;
  anchor.download = filename;
  document.body.append(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

function formatDateForFilename(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hour = String(date.getHours()).padStart(2, "0");
  const minute = String(date.getMinutes()).padStart(2, "0");

  return `${year}${month}${day}-${hour}${minute}`;
}

function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

export default App;
