import { defineConfig, loadEnv, type ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const adminApiTarget = env.VITE_ADMIN_API_TARGET
  const proxy: Record<string, string | ProxyOptions> = {}

  if (adminApiTarget) {
    proxy['/admin-api'] = {
      target: adminApiTarget,
      changeOrigin: true,
      ws: true,
      rewrite: (requestPath) => requestPath.replace(/^\/admin-api/, '')
    }
    proxy['/files'] = {
      target: adminApiTarget,
      changeOrigin: true,
      rewrite: (requestPath) => requestPath
    }
  }

  return {
    plugins: [vue()],
    server: {
      port: 6100,
      proxy
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src')
      }
    }
  }
})
