import { defineConfig, loadEnv, type ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const adminApiTarget = env.VITE_ADMIN_API_TARGET || 'http://127.0.0.1:6060'
  const proxy: Record<string, string | ProxyOptions> = {
    '/admin-api': {
      target: adminApiTarget,
      changeOrigin: true,
      ws: true,
      rewrite: (requestPath) => requestPath.replace(/^\/admin-api/, '')
    },
    '/files': {
      target: adminApiTarget,
      changeOrigin: true,
      rewrite: (requestPath) => requestPath
    }
  }

  return {
    base: './',
    plugins: [vue()],
    server: {
      port: 6200,
      proxy
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src')
      }
    }
  }
})
