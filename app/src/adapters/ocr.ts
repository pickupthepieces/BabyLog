import type { OcrResult } from "../domain/types";

export type OcrAdapter = {
  recognizeUltrasoundImage: (file: File) => Promise<OcrResult>;
};

export function createManualOnlyOcrAdapter(): OcrAdapter {
  return {
    async recognizeUltrasoundImage() {
      return {
        status: "manual-review-required",
        rawText: "",
        fields: {}
      };
    }
  };
}
