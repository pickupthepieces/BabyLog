export type UltrasoundFieldKey =
  | "examDate"
  | "gestationalAgeDays"
  | "bpdMm"
  | "hcMm"
  | "acMm"
  | "flMm"
  | "efwGram"
  | "afiMm"
  | "deepestVerticalPocketMm"
  | "placentaPosition"
  | "placentaGrade"
  | "fetalPresentation"
  | "umbilicalArterySd"
  | "umbilicalArteryPi"
  | "umbilicalArteryRi";

export type UltrasoundField = {
  key: UltrasoundFieldKey;
  label: string;
  unit: "date" | "days" | "mm" | "g" | "number" | "text";
  required: boolean;
  softRange?: readonly [number, number];
  ocrAliases: string[];
};

export const ULTRASOUND_FIELDS: UltrasoundField[] = [
  { key: "examDate", label: "检查日期", unit: "date", required: true, ocrAliases: ["检查日期", "日期", "Exam Date"] },
  {
    key: "gestationalAgeDays",
    label: "孕周",
    unit: "days",
    required: true,
    softRange: [70, 294],
    ocrAliases: ["孕周", "GA", "Gestational Age"]
  },
  { key: "bpdMm", label: "双顶径", unit: "mm", required: false, softRange: [10, 120], ocrAliases: ["BPD", "双顶径"] },
  { key: "hcMm", label: "头围", unit: "mm", required: false, softRange: [50, 400], ocrAliases: ["HC", "头围"] },
  { key: "acMm", label: "腹围", unit: "mm", required: false, softRange: [50, 400], ocrAliases: ["AC", "腹围"] },
  { key: "flMm", label: "股骨长", unit: "mm", required: false, softRange: [5, 90], ocrAliases: ["FL", "股骨长"] },
  { key: "efwGram", label: "估计胎重", unit: "g", required: false, softRange: [50, 6000], ocrAliases: ["EFW", "估重", "估计胎重"] },
  { key: "afiMm", label: "羊水指数", unit: "mm", required: false, softRange: [0, 400], ocrAliases: ["AFI", "羊水指数"] },
  {
    key: "deepestVerticalPocketMm",
    label: "羊水最大深度",
    unit: "mm",
    required: false,
    softRange: [0, 120],
    ocrAliases: ["MVP", "羊水最大深度", "最大羊水池"]
  },
  { key: "placentaPosition", label: "胎盘位置", unit: "text", required: false, ocrAliases: ["胎盘位置"] },
  { key: "placentaGrade", label: "胎盘成熟度", unit: "text", required: false, ocrAliases: ["胎盘成熟度", "胎盘分级"] },
  { key: "fetalPresentation", label: "胎位", unit: "text", required: false, ocrAliases: ["胎位", "胎方位"] },
  { key: "umbilicalArterySd", label: "脐动脉 S/D", unit: "number", required: false, softRange: [0, 10], ocrAliases: ["S/D", "脐动脉S/D"] },
  { key: "umbilicalArteryPi", label: "脐动脉 PI", unit: "number", required: false, softRange: [0, 3], ocrAliases: ["PI", "脐动脉PI"] },
  { key: "umbilicalArteryRi", label: "脐动脉 RI", unit: "number", required: false, softRange: [0, 1], ocrAliases: ["RI", "脐动脉RI"] }
];

export function getUltrasoundField(key: UltrasoundFieldKey): UltrasoundField | undefined {
  return ULTRASOUND_FIELDS.find((field) => field.key === key);
}

export function validateUltrasoundValue(
  key: UltrasoundFieldKey,
  value: number
): { ok: true } | { ok: false; code: "OUT_OF_SOFT_RANGE" } {
  const field = getUltrasoundField(key);

  if (!field?.softRange) {
    return { ok: true };
  }

  const [min, max] = field.softRange;
  return value >= min && value <= max ? { ok: true } : { ok: false, code: "OUT_OF_SOFT_RANGE" };
}
