<template>
  <div class="address-picker">
    <button class="address-trigger" type="button" @click="open">
      <span>{{ selectedLabel }}</span>
      <small>▾</small>
    </button>

    <div v-if="visible" class="address-mask" @click="close">
      <div class="address-sheet" @click.stop>
        <header class="address-header">
          <button type="button" class="address-close" @click="close">取消</button>
          <h3>选择收货地区</h3>
          <span class="address-count">{{ selectedPathNames.length ? '已选' : '请选择' }}</span>
        </header>

        <div class="address-search">
          <input
            ref="searchRef"
            v-model.trim="keyword"
            class="input"
            placeholder="搜索省 / 市 / 区县 / 镇街 / 村社区"
            inputmode="search"
            @input="search"
          />
        </div>

        <div v-if="selectedPathNames.length" class="address-selected">
          {{ selectedPathNames.join(' / ') }}
        </div>

        <div v-if="!keyword && availableInitials.length" class="letter-strip">
          <button
            v-for="letter in availableInitials"
            :key="letter"
            type="button"
            @click="jumpToInitial(letter)"
          >
            {{ letter }}
          </button>
        </div>

        <div v-if="keyword" class="address-results">
          <div v-if="loading" class="address-empty">搜索中...</div>
          <template v-else>
            <button
              v-for="item in searchResults"
              :key="item.code"
              type="button"
              class="address-result"
              @click="pickSearchResult(item)"
            >
              <strong>{{ item.name }}</strong>
              <span>{{ optionPathText(item) }}</span>
            </button>
            <div v-if="!searchResults.length" class="address-empty">没有匹配地址</div>
          </template>
        </div>

        <div v-else class="address-columns">
          <div v-for="(column, columnIndex) in columns" :key="columnIndex" ref="columnRefs" class="address-column">
            <button
              v-for="item in column"
              :key="item.code"
              type="button"
              :data-initial="optionInitial(item.name)"
              :class="['address-option', selectedPath[columnIndex] === item.code ? 'active' : '']"
              @click="pickColumnOption(item, columnIndex)"
            >
              {{ item.name }}
            </button>
          </div>
          <div v-if="!columns.length && loading" class="address-empty">加载中...</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { listAddressRegions, searchAddressRegions } from '@/api/shop'
import type { AddressRegionOption } from '@/types/shop'

const props = defineProps<{
  path: string[]
  pathNames: string[]
}>()
const emit = defineEmits<{
  (e: 'update:path', value: string[]): void
  (e: 'update:pathNames', value: string[]): void
}>()

const visible = ref(false)
const loading = ref(false)
const keyword = ref('')
const searchResults = ref<AddressRegionOption[]>([])
const columns = ref<AddressRegionOption[][]>([])
const selectedPath = ref<string[]>([...props.path])
const selectedPathNames = ref<string[]>([...props.pathNames])
const searchRef = ref<HTMLInputElement>()
const columnRefs = ref<HTMLElement[]>([])
let searchTimer: ReturnType<typeof window.setTimeout> | undefined

const selectedLabel = computed(() => selectedPathNames.value.length ? selectedPathNames.value.join(' / ') : '选择省 / 市 / 区县 / 镇街 / 村社区')
const activeColumnIndex = computed(() => Math.max(0, Math.min(selectedPath.value.length, columns.value.length - 1)))
const availableInitials = computed(() => {
  const records = columns.value[activeColumnIndex.value] || []
  const initials = new Set(records.map(item => optionInitial(item.name)).filter(item => item !== '#'))
  return ALPHABET.filter(item => initials.has(item))
})

const ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')
const PINYIN_BOUNDARIES = [
  ['A', '阿'], ['B', '八'], ['C', '嚓'], ['D', '咑'], ['E', '鵝'], ['F', '发'], ['G', '旮'], ['H', '哈'],
  ['J', '丌'], ['K', '咔'], ['L', '垃'], ['M', '妈'], ['N', '拿'], ['O', '噢'], ['P', '啪'], ['Q', '七'],
  ['R', '然'], ['S', '撒'], ['T', '他'], ['W', '挖'], ['X', '昔'], ['Y', '压'], ['Z', '匝']
] as const

watch(() => props.path, value => {
  selectedPath.value = [...value]
})
watch(() => props.pathNames, value => {
  selectedPathNames.value = [...value]
})
watch(visible, value => {
  document.body.style.overflow = value ? 'hidden' : ''
})

async function open() {
  visible.value = true
  keyword.value = ''
  searchResults.value = []
  await loadColumnsForPath(selectedPath.value)
  nextTick(() => searchRef.value?.focus())
}

function close() {
  visible.value = false
}

function commit(item: AddressRegionOption) {
  const path = item.path?.length ? item.path : [...selectedPath.value]
  const names = item.pathNames?.length ? item.pathNames : [...selectedPathNames.value]
  selectedPath.value = path
  selectedPathNames.value = names
  emit('update:path', [...path])
  emit('update:pathNames', [...names])
}

async function loadChildren(parentCode?: string) {
  return listAddressRegions(parentCode)
}

async function loadColumnsForPath(path: string[]) {
  loading.value = true
  try {
    const nextColumns: AddressRegionOption[][] = []
    let parentCode: string | undefined
    for (let index = 0; index <= path.length; index += 1) {
      const children = await loadChildren(parentCode)
      if (!children.length) {
        break
      }
      nextColumns.push(children)
      const selectedCode = path[index]
      if (!selectedCode || !children.some(item => item.code === selectedCode)) {
        break
      }
      parentCode = selectedCode
    }
    columns.value = nextColumns
  } finally {
    loading.value = false
  }
}

async function pickColumnOption(item: AddressRegionOption, columnIndex: number) {
  selectedPath.value = [...selectedPath.value.slice(0, columnIndex), item.code]
  selectedPathNames.value = [...selectedPathNames.value.slice(0, columnIndex), item.name]
  commit({
    ...item,
    path: item.path?.length ? item.path : [...selectedPath.value],
    pathNames: item.pathNames?.length ? item.pathNames : [...selectedPathNames.value]
  })

  loading.value = true
  try {
    const children = item.leaf ? [] : await loadChildren(item.code)
    columns.value = [...columns.value.slice(0, columnIndex + 1), ...(children.length ? [children] : [])]
    if (!children.length) {
      close()
    }
  } finally {
    loading.value = false
  }
}

function search() {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  if (!keyword.value.trim()) {
    searchResults.value = []
    return
  }
  searchTimer = window.setTimeout(async () => {
    loading.value = true
    try {
      searchResults.value = await searchAddressRegions(keyword.value.trim(), 30)
    } finally {
      loading.value = false
    }
  }, 240)
}

async function pickSearchResult(item: AddressRegionOption) {
  commit(item)
  keyword.value = ''
  searchResults.value = []
  await loadColumnsForPath(item.path || [item.code])
  const children = item.leaf ? [] : await loadChildren(item.code)
  if (children.length) {
    columns.value = [...columns.value.slice(0, (item.path || [item.code]).length), children]
  } else {
    close()
  }
}

function optionPathText(item: AddressRegionOption) {
  const names = item.pathNames?.length ? item.pathNames : [item.name]
  return names.join(' / ')
}

function jumpToInitial(letter: string) {
  const column = columnRefs.value[activeColumnIndex.value]
  const target = column?.querySelector(`[data-initial="${letter}"]`)
  target?.scrollIntoView({ block: 'start' })
}

function optionInitial(name?: string) {
  const first = String(name || '').trim().charAt(0)
  if (!first) {
    return '#'
  }
  if (/^[A-Za-z]$/.test(first)) {
    return first.toUpperCase()
  }
  for (let index = PINYIN_BOUNDARIES.length - 1; index >= 0; index -= 1) {
    const pair = PINYIN_BOUNDARIES[index]
    if (!pair) {
      continue
    }
    const letter = pair[0]
    const boundary = pair[1]
    if (first.localeCompare(boundary, 'zh-Hans-CN-u-co-pinyin') >= 0) {
      return letter
    }
  }
  return '#'
}
</script>

<style scoped>
.address-picker {
  width: 100%;
}

.address-trigger {
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

.address-trigger span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.address-trigger small {
  color: var(--text-sub);
}

.address-mask {
  position: fixed;
  inset: 0;
  z-index: 55;
  display: flex;
  align-items: flex-end;
  background: rgba(36, 27, 22, 0.5);
}

.address-sheet {
  width: 100%;
  max-width: 640px;
  max-height: 84vh;
  margin: 0 auto;
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

.address-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-soft);
}

.address-header h3 {
  margin: 0;
  color: var(--text-main);
  font-size: 16px;
  font-weight: 800;
}

.address-close,
.address-count {
  min-width: 48px;
  color: var(--text-sub);
  font-size: 13px;
}

.address-count {
  text-align: right;
}

.address-search {
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-soft);
}

.address-search .input {
  border-radius: var(--radius-pill);
  background: var(--bg-cream);
}

.address-selected {
  margin: 10px 16px 0;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  color: var(--brand-teal);
  background: #e6f2ef;
  font-size: 13px;
  font-weight: 700;
}

.letter-strip {
  display: flex;
  gap: 4px;
  overflow-x: auto;
  padding: 8px 16px 0;
}

.letter-strip button {
  min-width: 24px;
  height: 26px;
  border-radius: var(--radius-pill);
  color: var(--brand-teal);
  background: #e6f2ef;
  font-size: 12px;
  font-weight: 800;
}

.address-results,
.address-columns {
  flex: 1;
  min-height: 280px;
  overflow: hidden;
}

.address-results {
  overflow-y: auto;
  padding: 6px 16px 20px;
}

.address-result {
  display: grid;
  gap: 3px;
  width: 100%;
  margin: 6px 0;
  padding: 10px;
  border-radius: var(--radius);
  background: var(--bg-muted);
  text-align: left;
}

.address-result strong {
  color: var(--text-main);
  font-size: 14px;
}

.address-result span {
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.35;
}

.address-columns {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(118px, 1fr);
  gap: 1px;
  overflow-x: auto;
  padding: 10px 12px 20px;
}

.address-column {
  min-width: 118px;
  max-height: 46vh;
  overflow-y: auto;
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
}

.address-option {
  display: block;
  width: 100%;
  min-height: 38px;
  padding: 8px 10px;
  color: var(--text-main);
  background: transparent;
  text-align: left;
  font-size: 13px;
}

.address-option.active {
  color: var(--brand-teal);
  background: #e6f2ef;
  font-weight: 800;
}

.address-empty {
  padding: 32px 10px;
  color: var(--text-sub);
  text-align: center;
}
</style>
