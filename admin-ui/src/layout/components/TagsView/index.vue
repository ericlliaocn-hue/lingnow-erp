<template>
  <div class="tags-view-container">
    <div class="tags-view-wrapper">
      <router-link
        v-for="tag in visitedViews"
        :key="tag.path"
        :to="{ path: tag.path, query: tag.query }"
        class="tags-view-item"
        :class="isActive(tag) ? 'active' : ''"
        @contextmenu.prevent="openMenu(tag, $event)"
      >
        {{ tag.title }}
        <el-icon v-if="!isAffix(tag)" class="el-icon-close" @click.prevent.stop="closeSelectedTag(tag)">
          <Close />
        </el-icon>
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTagsViewStore } from '@/store/modules/tagsView'
import { Close } from '@element-plus/icons-vue'
import type { TagView } from '@/store/modules/tagsView'

const route = useRoute()
const router = useRouter()
const tagsViewStore = useTagsViewStore()

const visitedViews = computed(() => tagsViewStore.visitedViews)

const isActive = (tag: TagView) => {
  return tag.path === route.path
}

const isAffix = (tag: TagView) => {
  return tag.meta && tag.meta.affix
}

const addTags = () => {
  const { name } = route
  if (name) {
    tagsViewStore.addView(route)
  }
  return false
}

const closeSelectedTag = (view: TagView) => {
  tagsViewStore.delView(view).then((res: any) => {
    if (isActive(view)) {
      toLastView(res.visitedViews, view)
    }
  })
}

const toLastView = (visitedViews: TagView[], view: TagView) => {
  const latestView = visitedViews.slice(-1)[0]
  if (latestView) {
    router.push(latestView.fullPath as string)
  } else {
    // now the default is to redirect to the home page if there is no tags-view,
    // you can adjust it according to your needs.
    if (view.name === 'Dashboard') {
      // to reload home page
      router.replace({ path: '/redirect' + view.fullPath })
    } else {
      router.push('/')
    }
  }
}

const openMenu = (tag: TagView, e: MouseEvent) => {
  // TODO: Add context menu
}

watch(
  () => route.path,
  () => {
    addTags()
  }
)

onMounted(() => {
  addTags()
})
</script>

<style lang="scss" scoped>
.tags-view-container {
  min-height: 34px;
  height: auto;
  width: 100%;
  background-color: transparent;
  padding: 4px var(--app-spacing) 0;
  display: flex;
  align-items: center;

  .tags-view-wrapper {
    flex: 1;
    width: 100%;
    display: flex;
    flex-wrap: wrap;
    gap: 6px;

    .tags-view-item {
      display: inline-flex;
      align-items: center;
      position: relative;
      cursor: pointer;
      height: 28px;
      line-height: 26px;
      border: 1px solid rgba(145, 158, 171, 0.24);
      color: #637381;
      background: transparent;
      padding: 0 12px;
      font-size: 12px;
      border-radius: 6px;
      text-decoration: none;
      transition: all 0.2s;
      box-sizing: border-box;
      margin-bottom: 4px;

      &:hover {
        background-color: rgba(145, 158, 171, 0.08);
      }

      &.active {
        background-color: var(--el-color-primary);
        color: #fff;
        border-color: var(--el-color-primary);

        &::before {
          content: '';
          background: #fff;
          display: inline-block;
          width: 6px;
          height: 6px;
          border-radius: 50%;
          position: relative;
          margin-right: 6px;
        }
      }
    }
  }
}

:global(.dark) .tags-view-container {
  .tags-view-item {
    color: #919EAB;
    border-color: rgba(145, 158, 171, 0.12);
    
    &:hover {
      background-color: rgba(255, 255, 255, 0.04);
    }
    
    &.active {
      color: #fff;
      background-color: var(--el-color-primary);
      border-color: var(--el-color-primary);
    }
  }
}

.tags-view-wrapper {
  .tags-view-item {
    .el-icon-close {
      width: 16px;
      height: 16px;
      vertical-align: -2px;
      border-radius: 50%;
      text-align: center;
      transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
      transform-origin: 100% 50%;
      margin-left: 4px;

      &:before {
        transform: scale(0.6);
        display: inline-block;
        vertical-align: -3px;
      }

      &:hover {
        background-color: rgba(0, 0, 0, 0.12);
        color: #fff;
      }
    }
  }
}

:global(.dark) .tags-view-wrapper .tags-view-item .el-icon-close:hover {
  background-color: rgba(255, 255, 255, 0.24);
}
</style>
