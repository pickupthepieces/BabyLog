import type { ChildProfile } from "./types";

export type GestationalAge = {
  gestationalAgeDays: number;
  week: number;
  day: number;
  daysUntilDue: number;
};

export type BabyAge = {
  ageDays: number;
  ageWeeks: number;
  remainingDays: number;
};

export type CareStage =
  | ({ stage: "pregnancy" } & GestationalAge)
  | ({ stage: "baby" } & BabyAge)
  | { stage: "unknown" };

const GESTATION_DAYS_AT_DUE_DATE = 280;

export function calculateGestationalAge(expectedDueDate: string, atDate: string): GestationalAge {
  const daysUntilDue = differenceInDateOnlyDays(atDate, expectedDueDate);
  const gestationalAgeDays = GESTATION_DAYS_AT_DUE_DATE - daysUntilDue;

  return {
    gestationalAgeDays,
    week: Math.floor(gestationalAgeDays / 7),
    day: gestationalAgeDays % 7,
    daysUntilDue
  };
}

export function calculateBabyAge(birthDate: string, atDate: string): BabyAge {
  const ageDays = differenceInDateOnlyDays(birthDate, atDate);

  return {
    ageDays,
    ageWeeks: Math.floor(ageDays / 7),
    remainingDays: ageDays % 7
  };
}

export function getCareStage(profile: ChildProfile, atDate: string): CareStage {
  if (profile.birthDate && differenceInDateOnlyDays(profile.birthDate, atDate) >= 0) {
    return {
      stage: "baby",
      ...calculateBabyAge(profile.birthDate, atDate)
    };
  }

  if (profile.expectedDueDate) {
    return {
      stage: "pregnancy",
      ...calculateGestationalAge(profile.expectedDueDate, atDate)
    };
  }

  return { stage: "unknown" };
}

function differenceInDateOnlyDays(startDate: string, endDate: string): number {
  const start = parseDateOnlyAsUtc(startDate);
  const end = parseDateOnlyAsUtc(endDate);

  return Math.round((end - start) / 86_400_000);
}

function parseDateOnlyAsUtc(value: string): number {
  const [year, month, day] = value.slice(0, 10).split("-").map(Number);

  return Date.UTC(year, month - 1, day);
}
