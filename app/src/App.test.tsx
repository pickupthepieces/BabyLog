import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import App from "./App";

describe("BabyLog UI shell", () => {
  beforeEach(async () => {
    vi.restoreAllMocks();
    await new Promise<void>((resolve, reject) => {
      const request = indexedDB.deleteDatabase("babylog-local");
      request.onsuccess = () => resolve();
      request.onerror = () => reject(request.error);
      request.onblocked = () => resolve();
    });
    vi.spyOn(URL, "createObjectURL").mockReturnValue("blob:babylog-backup");
    vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => undefined);
    vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => undefined);
  });

  it("renders the pregnancy home workspace", () => {
    render(<App />);

    expect(screen.getByRole("heading", { name: /BabyLog/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /孕 28/ })).toHaveTextContent("孕 28+3 周");
    expect(screen.getByText(/距预产期 82 天/)).toBeInTheDocument();
    expect(screen.getByText(/最近记录/)).toBeInTheDocument();
  });

  it("keeps developer platform constraints out of the home UI", () => {
    render(<App />);

    expect(screen.queryByText(/iOS 优先验证/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Android 紧跟回归/i)).not.toBeInTheDocument();
    expect(screen.getByText("今日记录")).toBeInTheDocument();
  });

  it("shows primary bottom navigation labels", () => {
    render(<App />);

    for (const label of ["首页", "时间线", "资料", "设置"]) {
      expect(screen.getByRole("button", { name: label })).toBeInTheDocument();
    }
    expect(screen.getByRole("button", { name: "快捷记录" })).toBeInTheDocument();
  });

  it("opens the quick record sheet from the center action", () => {
    render(<App />);

    fireEvent.click(screen.getByRole("button", { name: "快捷记录" }));

    expect(screen.getByRole("dialog", { name: "快捷记录" })).toBeInTheDocument();
    for (const label of ["喂养", "睡眠", "尿布", "体温", "用药", "B超"]) {
      expect(screen.getByRole("button", { name: new RegExp(label) })).toBeInTheDocument();
    }
  });

  it("switches to timeline, library, and settings workspaces", () => {
    render(<App />);

    fireEvent.click(screen.getByRole("button", { name: "时间线" }));
    expect(screen.getByRole("heading", { name: "时间线" })).toBeInTheDocument();
    expect(screen.getByText(/全部/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "资料" }));
    expect(screen.getByRole("heading", { name: "资料" })).toBeInTheDocument();
    expect(screen.getByText(/B 超单/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "设置" }));
    expect(screen.getByRole("heading", { name: "设置" })).toBeInTheDocument();
    expect(screen.getByText(/后端未配置/)).toBeInTheDocument();
  });

  it("shows local mode on home and backend status in settings", () => {
    render(<App />);

    expect(screen.getByText(/本机模式/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "设置" }));

    expect(screen.getByText(/后端未配置/i)).toBeInTheDocument();
  });

  it("records quick actions locally and exposes pending sync state", async () => {
    render(<App />);

    fireEvent.click(screen.getByRole("button", { name: "快捷记录" }));
    fireEvent.click(screen.getByRole("button", { name: /喂养/ }));

    expect(await screen.findByText(/喂养已保存到本机/)).toBeInTheDocument();
    expect(await screen.findByText("1 条")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "时间线" }));

    expect(await screen.findByText("喂养")).toBeInTheDocument();
    expect(screen.getByText(/快捷记录/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "设置" }));

    expect(await screen.findByText("待同步记录")).toBeInTheDocument();
    expect(screen.getByText("1 条待上传")).toBeInTheDocument();
  });

  it("exports local records as a JSON backup", async () => {
    render(<App />);

    fireEvent.click(screen.getByRole("button", { name: "快捷记录" }));
    fireEvent.click(screen.getByRole("button", { name: /尿布/ }));
    await screen.findByText(/尿布已保存到本机/);

    fireEvent.click(screen.getByRole("button", { name: "设置" }));
    fireEvent.click(screen.getByRole("button", { name: "导出" }));

    await waitFor(() => expect(URL.createObjectURL).toHaveBeenCalled());
    expect(await screen.findByText(/备份已生成/)).toBeInTheDocument();
  });

  it("exports ultrasound image blobs in the local backup", async () => {
    render(<App />);

    fireEvent.click(screen.getByRole("button", { name: "快捷记录" }));
    fireEvent.click(screen.getByRole("button", { name: /B超/ }));

    await screen.findByRole("dialog", { name: "B 超记录" });
    fireEvent.change(screen.getByLabelText("B 超单照片"), {
      target: {
        files: [new File(["scan-image"], "scan.jpg", { type: "image/jpeg" })]
      }
    });
    fireEvent.click(screen.getByRole("button", { name: "保存 B 超记录" }));
    await screen.findByText(/B 超已保存到本机/);

    fireEvent.click(screen.getByRole("button", { name: "设置" }));
    fireEvent.click(screen.getByRole("button", { name: "导出" }));

    await waitFor(() => expect(URL.createObjectURL).toHaveBeenCalled());
    const lastCreateUrlCall = vi.mocked(URL.createObjectURL).mock.calls.at(-1);
    const backupBlob = lastCreateUrlCall?.[0] as Blob;
    const backup = JSON.parse(await readBlobText(backupBlob));

    expect(backup.data.attachmentBlobs).toEqual([
      expect.objectContaining({
        attachmentId: expect.stringMatching(/^att_/),
        mimeType: "image/jpeg",
        dataBase64: "c2Nhbi1pbWFnZQ=="
      })
    ]);
  });

  it("records ultrasound measurements and a scan image locally", async () => {
    render(<App />);

    fireEvent.click(screen.getByRole("button", { name: "快捷记录" }));
    fireEvent.click(screen.getByRole("button", { name: /B超/ }));

    expect(await screen.findByRole("dialog", { name: "B 超记录" })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("检查日期"), { target: { value: "2026-05-15" } });
    fireEvent.change(screen.getByLabelText("孕周"), { target: { value: "28+3" } });
    fireEvent.change(screen.getByLabelText("双顶径 BPD"), { target: { value: "71" } });
    fireEvent.change(screen.getByLabelText("头围 HC"), { target: { value: "258" } });
    fireEvent.change(screen.getByLabelText("腹围 AC"), { target: { value: "235" } });
    fireEvent.change(screen.getByLabelText("股骨长 FL"), { target: { value: "54" } });
    fireEvent.change(screen.getByLabelText("估计胎重 EFW"), { target: { value: "1420" } });
    fireEvent.change(screen.getByLabelText("B 超单照片"), {
      target: {
        files: [new File(["scan-image"], "scan.jpg", { type: "image/jpeg" })]
      }
    });
    fireEvent.click(screen.getByRole("button", { name: "保存 B 超记录" }));

    expect(await screen.findByText(/B 超已保存到本机/)).toBeInTheDocument();
    expect(await screen.findByText("1420 g")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "时间线" }));

    const timeline = await screen.findByLabelText("时间线记录");
    expect(await within(timeline).findByText("B 超")).toBeInTheDocument();
    expect(await within(timeline).findByText("28+3 周 · EFW 1420 g · BPD 71 mm")).toBeInTheDocument();
    expect(await within(timeline).findByLabelText("含 1 张附件")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "设置" }));

    expect(await screen.findByText("2 条待上传")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "资料" }));

    const library = await screen.findByLabelText("资料分类");
    expect(within(library).getByText("B 超单")).toBeInTheDocument();
    expect(await within(library).findByText("1 张")).toBeInTheDocument();

    fireEvent.click(within(library).getByText("B 超单"));

    const attachmentList = await screen.findByRole("dialog", { name: "B 超单列表" });
    expect(within(attachmentList).getByText("scan.jpg")).toBeInTheDocument();
    expect(within(attachmentList).getByText("10 B")).toBeInTheDocument();
    expect(within(attachmentList).getByText("OCR 未启用")).toBeInTheDocument();

    fireEvent.click(within(attachmentList).getByText("scan.jpg"));

    const preview = await screen.findByRole("dialog", { name: "scan.jpg 预览" });
    expect(within(preview).getByAltText("scan.jpg")).toHaveAttribute("src", "blob:babylog-backup");
  });

  it("keeps an invalid ultrasound form from creating pending records", async () => {
    render(<App />);

    fireEvent.click(screen.getByRole("button", { name: "快捷记录" }));
    fireEvent.click(screen.getByRole("button", { name: /B超/ }));

    expect(await screen.findByRole("dialog", { name: "B 超记录" })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("孕周"), { target: { value: "28+x" } });
    fireEvent.click(screen.getByRole("button", { name: "保存 B 超记录" }));

    expect(await screen.findByText(/请填写有效孕周/)).toBeInTheDocument();
    expect(screen.getByRole("dialog", { name: "B 超记录" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "关闭" }));
    fireEvent.click(screen.getByRole("button", { name: "设置" }));

    expect(await screen.findByText("0 条待上传")).toBeInTheDocument();
  });
});

function readBlobText(blob: Blob): Promise<string> {
  if ("text" in blob && typeof blob.text === "function") {
    return blob.text();
  }

  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error);
    reader.readAsText(blob);
  });
}
