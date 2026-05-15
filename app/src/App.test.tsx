import { fireEvent, render, screen } from "@testing-library/react";
import App from "./App";

describe("BabyLog UI shell", () => {
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
    expect(screen.getByText("上次胎动")).toBeInTheDocument();
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
});
