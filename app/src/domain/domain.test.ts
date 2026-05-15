import { describe, expect, it } from "vitest";
import { createEvent, EVENT_TYPES, isEventType } from "./types";
import { getUltrasoundField, ULTRASOUND_FIELDS, validateUltrasoundValue } from "./ultrasound";

describe("domain model", () => {
  it("keeps event types explicit", () => {
    expect(EVENT_TYPES).toContain("ultrasound");
    expect(EVENT_TYPES).toContain("feed");
    expect(isEventType("growth")).toBe(true);
    expect(isEventType("unknown")).toBe(false);
  });

  it("creates sync-ready events with metadata", () => {
    const event = createEvent({
      familyId: "family-1",
      childId: "child-1",
      eventType: "temperature",
      occurredAt: "2026-05-15T08:00:00+08:00",
      payload: { valueCelsius: 37.2 }
    });

    expect(event.id).toMatch(/^evt_/);
    expect(event.familyId).toBe("family-1");
    expect(event.updatedBy).toBe("local");
    expect(event.schemaVersion).toBe(1);
    expect(event.deletedAt).toBeNull();
  });

  it("defines ultrasound fields with units and OCR aliases", () => {
    const bpd = getUltrasoundField("bpdMm");

    expect(ULTRASOUND_FIELDS.length).toBeGreaterThan(8);
    expect(bpd?.unit).toBe("mm");
    expect(bpd?.ocrAliases).toContain("双顶径");
  });

  it("uses ultrasound field ranges as warnings, not hard diagnosis", () => {
    expect(validateUltrasoundValue("bpdMm", 60)).toEqual({ ok: true });
    expect(validateUltrasoundValue("bpdMm", 150)).toEqual({
      ok: false,
      code: "OUT_OF_SOFT_RANGE"
    });
  });
});
