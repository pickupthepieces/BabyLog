import { describe, expect, it } from "vitest";
import { createChildProfile, createFamilyProfile } from "./types";
import { calculateBabyAge, calculateGestationalAge, getCareStage } from "./stage";

describe("family and child profile model", () => {
  it("creates a family profile and a single visible child profile", () => {
    const family = createFamilyProfile({ displayName: "我们家" });
    const child = createChildProfile({
      familyId: family.id,
      displayName: "宝宝",
      expectedDueDate: "2026-10-01"
    });

    expect(family.id).toMatch(/^fam_/);
    expect(family.familyId).toBe(family.id);
    expect(child.id).toMatch(/^child_/);
    expect(child.familyId).toBe(family.id);
    expect(child.expectedDueDate).toBe("2026-10-01");
    expect(child.birthDate).toBeNull();
    expect(child.isCurrentSingleton).toBe(true);
  });

  it("keeps the data model ready for one family to own multiple child records", () => {
    const familyId = "family-1";
    const first = createChildProfile({ familyId, displayName: "宝宝A" });
    const second = createChildProfile({ familyId, displayName: "宝宝B" });

    expect(first.familyId).toBe(second.familyId);
    expect(first.id).not.toBe(second.id);
  });
});

describe("stage calculations", () => {
  it("calculates gestational age from expected due date", () => {
    expect(calculateGestationalAge("2026-10-01", "2026-10-01")).toEqual({
      gestationalAgeDays: 280,
      week: 40,
      day: 0,
      daysUntilDue: 0
    });

    expect(calculateGestationalAge("2026-10-01", "2026-09-24")).toMatchObject({
      gestationalAgeDays: 273,
      week: 39,
      day: 0,
      daysUntilDue: 7
    });
  });

  it("calculates baby age after birth", () => {
    expect(calculateBabyAge("2026-05-01", "2026-05-15")).toEqual({
      ageDays: 14,
      ageWeeks: 2,
      remainingDays: 0
    });
  });

  it("prefers baby stage only after a birth date exists", () => {
    const pregnancy = createChildProfile({
      familyId: "family-1",
      displayName: "宝宝",
      expectedDueDate: "2026-10-01"
    });
    const born = createChildProfile({
      familyId: "family-1",
      displayName: "宝宝",
      expectedDueDate: "2026-10-01",
      birthDate: "2026-05-01"
    });

    expect(getCareStage(pregnancy, "2026-05-15").stage).toBe("pregnancy");
    expect(getCareStage(born, "2026-05-15").stage).toBe("baby");
  });
});
