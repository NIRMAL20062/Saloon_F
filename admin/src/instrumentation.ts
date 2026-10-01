/** Runs once when the server starts: refuse to start with a missing or invalid configuration. */
export async function register() {
  if (process.env.NEXT_RUNTIME === "nodejs") {
    const { getEnv } = await import("./env");
    try {
      getEnv();
    } catch (error) {
      // Exit instead of serving broken pages, so the host (or the developer) sees the failure immediately.
      console.error(error instanceof Error ? error.message : error);
      process.exit(1);
    }
  }
}
