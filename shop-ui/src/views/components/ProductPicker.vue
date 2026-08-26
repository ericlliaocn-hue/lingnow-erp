<template>
  <div class="picker">
    <button class="picker-trigger" type="button" @click="open">
      <span>{{ selectedLabel }}</span>
      <small>▾</small>
    </button>

    <div v-if="visible" class="picker-mask" @click="close">
      <div class="picker-sheet" @click.stop>
        <header class="picker-header">
          <button type="button" class="picker-close" @click="close">取消</button>
          <h3>选择商品</h3>
          <span class="picker-count">{{ filtered.length }} 款</span>
        </header>
        <div class="picker-search">
          <input
            ref="searchRef"
            v-model.trim="keyword"
            class="input"
            placeholder="搜索商品名称、规格"
            inputmode="search"
          />
        </div>
        <div class="picker-list">
          <div v-if="filtered.length === 0" class="empty">没有找到相关商品</div>
          <button
            v-for="item in filtered"
            :key="item.id"
            type="button"
            :class="['picker-option', String(item.id) === String(modelValue) ? 'active' : '']"
            @click="pick(item)"
          >
            <img v-if="item.imageUrl" :src="item.imageUrl" alt="" loading="lazy" />
            <div v-else class="option-empty">荣时</div>
            <div class="option-info">
              <strong>{{ item.name }}</strong>
              <p>{{ item.spec || '多规格可选' }}</p>
            </div>
            <span class="option-price">{{ priceLabel(item.salePrice) }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { priceLabel } from '@/utils/label'
import type { ShopProduct } from '@/types/shop'

const props = defineProps<{
  modelValue: string
  products: ShopProduct[]
}>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const visible = ref(false)
const keyword = ref('')
const searchRef = ref<HTMLInputElement>()

const selected = computed(() => props.products.find(item => String(item.id) === String(props.modelValue)))
const selectedLabel = computed(() => {
  if (!selected.value) return '请选择商品'
  const name = selected.value.name
  return selected.value.spec ? `${name} · ${selected.value.spec}` : name
})
const filtered = computed(() => {
  const kw = keyword.value.toLowerCase()
  if (!kw) return props.products
  return props.products.filter(item => {
    const text = `${item.name || ''} ${item.spec || ''} ${item.attributeText || ''}`.toLowerCase()
    return text.includes(kw)
  })
})

function open() {
  visible.value = true
  keyword.value = ''
  nextTick(() => searchRef.value?.focus())
}
function close() {
  visible.value = false
}
function pick(item: ShopProduct) {
  emit('update:modelValue', String(item.id))
  close()
}

watch(visible, (val) => {
  document.body.style.overflow = val ? 'hidden' : ''
})
</script>

<style scoped>
.picker {
  width: 100%;
}

.picker-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  min-height: 42px;
  padding: 9px 12px;
  border: 1px solid var(--border-line);
  border-radius: var(--radius-sm);
  color: var(--text-main);
  background: #fff;
  text-align: left;
}

.picker-trigger small {
  color: var(--text-sub);
}

.picker-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(36, 27, 22, 0.5);
  display: flex;
  align-items: flex-end;
}

.picker-sheet {
  width: 100%;
  max-width: 640px;
  margin: 0 auto;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  background: #fff;
  box-shadow: 0 -10px 30px rgba(0, 0, 0, 0.15);
  animation: sheet-up 0.25s ease;
}

@keyframes sheet-up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-soft);
}

.picker-header h3 {
  margin: 0;
  color: var(--text-main);
  font-size: 16px;
  font-weight: 800;
}

.picker-close {
  color: var(--text-sub);
  font-size: 14px;
}

.picker-count {
  color: var(--text-sub);
  font-size: 12px;
}

.picker-search {
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-soft);
}

.picker-search .input {
  border-radius: var(--radius-pill);
  background: var(--bg-cream);
}

.picker-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 16px 20px;
}

.picker-option {
  display: grid;
  grid-template-columns: 56px 1fr auto;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px;
  margin: 6px 0;
  border: 1px solid transparent;
  border-radius: var(--radius);
  background: var(--bg-muted);
  text-align: left;
}

.picker-option.active {
  border-color: var(--brand-teal);
  background: #e6f2ef;
}

.picker-option img,
.option-empty {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-sm);
  object-fit: contain;
  background: #fff;
}

.option-empty {
  display: grid;
  place-items: center;
  color: #9b826b;
  font-size: 11px;
  font-weight: 800;
}

.option-info {
  min-width: 0;
}

.option-info strong {
  display: block;
  color: var(--text-main);
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.option-info p {
  margin: 4px 0 0;
  color: var(--text-sub);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.option-price {
  color: var(--brand-orange);
  font-size: 14px;
  font-weight: 800;
}
</style>
