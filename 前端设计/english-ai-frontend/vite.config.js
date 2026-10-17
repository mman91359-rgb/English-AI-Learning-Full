import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/user': 'http://localhost:8086',
      '/exercise': 'http://localhost:8086',
      '/learning': 'http://localhost:8086'
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets'
  }
})
