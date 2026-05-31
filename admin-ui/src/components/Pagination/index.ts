import { type App } from 'vue'
import Pagination from './index.vue'

export default {
  install(app: App) {
    app.component('Pagination', Pagination)
  }
}
