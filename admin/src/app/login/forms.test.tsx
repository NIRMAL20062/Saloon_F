import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import { AuthenticatorSetup, CodeForm, EmailForm } from "./forms";

vi.mock("./actions", () => ({}));

const submit = (button: string) => fireEvent.click(screen.getByRole("button", { name: button }));

describe("EmailForm", () => {
  test("asks for the email", () => {
    render(<EmailForm action={vi.fn()} />);

    expect(screen.getByLabelText("Email")).toHaveAttribute("type", "email");
    expect(screen.getByRole("button", { name: "Send code" })).toBeEnabled();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  test("sends what was typed and shows the action's error", async () => {
    const action = vi.fn(async () => ({ error: "Enter a valid email address." }));
    render(<EmailForm action={action} />);

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "nope" } });
    submit("Send code");

    expect(await screen.findByRole("alert")).toHaveTextContent("Enter a valid email address.");
    expect((action.mock.calls[0] as unknown as [unknown, FormData])[1].get("email")).toBe("nope");
  });

  test("shows that it's sending while the action runs", async () => {
    let finish: (state: object) => void = () => {};
    render(<EmailForm action={() => new Promise((resolve) => (finish = resolve))} />);

    submit("Send code");

    expect(await screen.findByRole("button", { name: "Sending…" })).toBeDisabled();
    // React 19 queues later actions behind an unfinished one, so let it end before the next test.
    finish({});
    expect(await screen.findByRole("button", { name: "Send code" })).toBeEnabled();
  });
});

describe("CodeForm", () => {
  test("a numeric one-time-code field, so phones offer the code from the email", () => {
    render(<CodeForm action={vi.fn()} label="Code from the email" />);

    const input = screen.getByLabelText("Code from the email");
    expect(input).toHaveAttribute("autocomplete", "one-time-code");
    expect(input).toHaveAttribute("inputmode", "numeric");
    expect(input).toHaveAttribute("maxlength", "6");
  });

  test("shows a wrong-code error", async () => {
    render(<CodeForm action={async () => ({ error: "That code is wrong or has expired." })} label="Code" submitLabel="Log in" />);

    submit("Log in");

    expect(await screen.findByRole("alert")).toHaveTextContent("That code is wrong or has expired.");
  });
});

describe("AuthenticatorSetup", () => {
  const enrollment = { factorId: "f-1", qrCode: "data:image/svg+xml;base64,PHN2Zy8+", secret: "JBSWY3DPEHPK3PXP" };

  test("starts with one button", () => {
    render(<AuthenticatorSetup action={vi.fn()} />);

    expect(screen.getByRole("button", { name: "Set up authenticator app" })).toBeEnabled();
    expect(screen.queryByRole("img")).toBeNull();
  });

  test("then shows the QR code, the key to type, and asks for the first code", async () => {
    render(<AuthenticatorSetup action={async () => ({ enrollment })} />);

    submit("Set up authenticator app");

    expect(await screen.findByRole("img", { name: "QR code for your authenticator app" })).toHaveAttribute("src", enrollment.qrCode);
    expect(screen.getByText(enrollment.secret)).toBeInTheDocument();
    expect(screen.getByLabelText("Code from the app")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Finish setup" })).toBeEnabled();
  });

  test("a wrong first code keeps the QR code and shows the error", async () => {
    let call = 0;
    render(<AuthenticatorSetup action={async () => (++call === 1 ? { enrollment } : { enrollment, error: "That code is wrong." })} />);

    submit("Set up authenticator app");
    submit(await screen.findByRole("button", { name: "Finish setup" }).then((b) => b.textContent!));

    expect(await screen.findByRole("alert")).toHaveTextContent("That code is wrong.");
    expect(screen.getByRole("img", { name: "QR code for your authenticator app" })).toBeInTheDocument();
  });

  test("an error before the QR code shows with the start button", async () => {
    render(<AuthenticatorSetup action={async () => ({ error: "Login isn't working right now." })} />);

    submit("Set up authenticator app");

    expect(await screen.findByRole("alert")).toHaveTextContent("Login isn't working right now.");
  });
});
