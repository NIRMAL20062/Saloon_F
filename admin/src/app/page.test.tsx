import { render, screen } from "@testing-library/react";
import { expect, test } from "vitest";
import Home from "./page";

test("start page shows the panel name", () => {
  render(<Home />);

  expect(screen.getByRole("heading", { name: "Glide Admin" })).toBeInTheDocument();
});
