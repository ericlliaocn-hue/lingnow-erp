import {defineConfig, loadEnv} from "vite";
import uni from "@dcloudio/vite-plugin-uni";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const appApiTarget = env.VITE_APP_API_PROXY_TARGET || env.VITE_APP_API_BASE_URL

  return {
    plugins: [uni()],
    server: {
      port: 6101,
      proxy: appApiTarget ? {
        '/app-api': {
          target: appApiTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/app-api/, '')
        }
      } : undefined
    }
  }
});
