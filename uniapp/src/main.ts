import { createSSRApp } from "vue";
import App from "./App.vue";

const enforceProductionHttps = () => {
  if (
    import.meta.env.PROD &&
    typeof window !== "undefined" &&
    window.location.protocol === "http:" &&
    window.location.hostname.endsWith(".oioio.chat")
  ) {
    window.location.replace(`https://${window.location.host}${window.location.pathname}${window.location.search}${window.location.hash}`);
  }
};
enforceProductionHttps();

export function createApp() {
  const app = createSSRApp(App);
  return {
    app,
  };
}
