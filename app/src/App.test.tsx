import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import App from "./App";

describe("BabyLog UI shell", () => {
  beforeEach(async () => {
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
});
