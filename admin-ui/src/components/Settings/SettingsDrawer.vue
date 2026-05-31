<template>
  <el-drawer
    v-model="store.openDrawer"
    :title="null"
    :with-header="false"
    size="360px"
    direction="rtl"
    class="settings-drawer"
    :append-to-body="true"
  >
    <!-- Header -->
    <div class="drawer-header">
      <span class="header-title">Settings</span>
      <div class="header-actions">
        <!-- Reset Button -->
        <div class="icon-btn refresh-btn" @click="store.resetSettings">
             <el-icon class="refresh-icon"><RefreshRight /></el-icon>
             <div class="refresh-badge" v-if="isModified"></div>
        </div>
        <!-- Close Button -->
        <div class="icon-btn" @click="store.setDrawerOpen(false)">
             <el-icon class="close-icon"><Close /></el-icon>
        </div>
      </div>
    </div>

    <!-- Content -->
    <el-scrollbar>
      <div class="drawer-content">
        
        <!-- Mode & Contrast & Direction & Compact Grid -->
        <div class="control-grid">
            <!-- Mode -->
           <div class="control-card">
              <div class="control-icon"><ModeIcon /></div>
              <span class="control-label">Mode</span>
              <el-switch 
                v-model="isDark" 
                style="--el-switch-on-color: #00A76F; --el-switch-off-color: #919eab;"
              />
           </div>
           <!-- Contrast -->
           <div class="control-card">
              <div class="control-icon"><ContrastIcon /></div>
              <span class="control-label">Contrast</span>
              <el-switch 
                v-model="isContrast" 
                style="--el-switch-on-color: #00A76F; --el-switch-off-color: #919eab;"
              />
           </div>
           <!-- Direction -->
           <div class="control-card">
              <div class="control-icon"><DirectionIcon /></div>
              <span class="control-label">Right to left</span>
              <el-switch 
                v-model="isRTL" 
                style="--el-switch-on-color: #00A76F; --el-switch-off-color: #919eab;"
              />
           </div>
           <!-- Compact -->
            <div class="control-card">
              <div class="control-icon"><CompactIcon /></div>
              <span class="control-label">Compact</span>
              <el-switch 
                v-model="store.settings.compactLayout" 
                style="--el-switch-on-color: #00A76F; --el-switch-off-color: #919eab;"
              />
           </div>
        </div>

         <!-- Nav Layout -->
         <div class="block-section">
            <div class="block-header">
                <span class="block-title">Nav</span>
            </div>
            
            <div class="sub-label">Layout</div>
            <div class="layout-grid">
                <div class="layout-item" :class="{ active: store.settings.navLayout === 'vertical' }" @click="store.settings.navLayout = 'vertical'"> 
                    <NavVerticalIcon class="layout-svg" />
                </div>
                <div class="layout-item" :class="{ active: store.settings.navLayout === 'horizontal' }" @click="store.settings.navLayout = 'horizontal'">
                    <NavHorizontalIcon class="layout-svg" />
                </div>
                <div class="layout-item" :class="{ active: store.settings.navLayout === 'mini' }" @click="store.settings.navLayout = 'mini'">
                    <NavMiniIcon class="layout-svg" />
                </div>
            </div>

             <div class="sub-label" style="margin-top: 24px;">Color</div>
             <div class="color-grid">
                 <div class="color-card" :class="{ active: store.settings.navColor === 'integrate' }" @click="store.settings.navColor = 'integrate'">
                    <div class="color-visual integrate"></div>
                    <span>Integrate</span>
                 </div>
                 <div class="color-card" :class="{ active: store.settings.navColor === 'apparent' }" @click="store.settings.navColor = 'apparent'">
                     <div class="color-visual apparent"></div>
                    <span>Apparent</span>
                 </div>
             </div>
         </div>

    
          <!-- Presets -->
        <div class="block-section">
             <div class="block-header">
                <span class="block-title">Presets</span>
            </div>
            <div class="presets-grid">
                <div 
                    v-for="(color, key) in store.primaryColorPresets" 
                    :key="key"
                    class="preset-item"
                    :class="{ active: store.settings.primaryColor === key }"
                    :style="store.settings.primaryColor === key ? { 
                        boxShadow: `0 8px 16px -4px ${hexToRgba(color, 0.24)}`,
                        backgroundColor: hexToRgba(color, 0.08)
                    } : {}"
                    @click="store.settings.primaryColor = key"
                >
                    <div class="preset-circle" :style="{ backgroundColor: color }">
                        <!-- Optional: Add check or icon if needed, but per reference just color scaling/shadow is key -->
                    </div>
                </div>
            </div>
        </div>

        <!-- Font -->
         <div class="block-section">
             <div class="block-header">
                <span class="block-title">Font</span>
            </div>
            <div class="sub-label">Family</div>
            <div class="font-grid">
                 <div class="font-card" :class="{ active: store.settings.fontFamily === 'Public Sans' }" @click="store.settings.fontFamily = 'Public Sans'">
                    <span style="font-family: 'Public Sans', sans-serif; font-size: 20px; font-weight: 700;">Aa</span>
                    <span class="font-name">Public Sans</span>
                 </div>
                 <div class="font-card" :class="{ active: store.settings.fontFamily === 'Inter' }" @click="store.settings.fontFamily = 'Inter'">
                    <span style="font-family: 'Inter', sans-serif; font-size: 20px; font-weight: 700;">Aa</span>
                    <span class="font-name">Inter</span>
                 </div>
                  <div class="font-card" :class="{ active: store.settings.fontFamily === 'DM Sans' }" @click="store.settings.fontFamily = 'DM Sans'">
                    <span style="font-family: 'DM Sans', sans-serif; font-size: 20px; font-weight: 700;">Aa</span>
                    <span class="font-name">DM Sans</span>
                 </div>
                 <div class="font-card" :class="{ active: store.settings.fontFamily === 'Nunito Sans' }" @click="store.settings.fontFamily = 'Nunito Sans'">
                    <span style="font-family: 'Nunito Sans', sans-serif; font-size: 20px; font-weight: 700;">Aa</span>
                    <span class="font-name">Nunito Sans</span>
                 </div>
            </div>

            <div class="sub-label" style="margin-top: 24px;">Size</div>
            <div class="font-size-slider">
                 <el-slider 
                    v-model="store.settings.fontSize" 
                    :min="12" 
                    :max="20"
                    :step="1"
                    show-stops
                    class="custom-slider"
                 />
                 <div class="slider-value">{{ store.settings.fontSize }}px</div>
            </div>
         </div>

      </div>
    </el-scrollbar>

  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useSettingsStore } from '@/store/modules/settings'
import { RefreshRight, Close } from '@element-plus/icons-vue'
import { 
    ModeIcon, ContrastIcon, DirectionIcon, CompactIcon, 
    NavVerticalIcon, NavHorizontalIcon, NavMiniIcon,
    SidebarOutlineIcon, SidebarFillIcon, PresetsIcon
} from './icons'

const store = useSettingsStore()

const isDark = computed({
    get: () => store.settings.themeMode === 'dark',
    set: (val) => store.settings.themeMode = val ? 'dark' : 'light'
})

const isContrast = computed({
    get: () => store.settings.contrast === 'bold',
    set: (val) => store.settings.contrast = val ? 'bold' : 'default'
})

const isRTL = computed({
    get: () => store.settings.direction === 'rtl',
    set: (val) => store.settings.direction = val ? 'rtl' : 'ltr'
})

const isModified = computed(() => {
    return store.settings.themeMode !== 'light' || store.settings.primaryColor !== 'default'
})

// Quick helper to convert hex to rgba
function hexToRgba(hex: string, alpha: number) {
    const r = parseInt(hex.slice(1, 3), 16)
    const g = parseInt(hex.slice(3, 5), 16)
    const b = parseInt(hex.slice(5, 7), 16)
    return `rgba(${r}, ${g}, ${b}, ${alpha})`
}
</script>

<style scoped>
.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
}

.header-title {
  font-weight: 700;
  font-size: 18px;
  color: var(--el-text-color-primary);
}

.header-actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--el-text-color-regular);
}
.icon-btn:hover {
  background-color: var(--el-fill-color-light);
}

.refresh-btn {
    position: relative;
}
.refresh-badge {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 6px;
    height: 6px;
    background-color: #FF5630;
    border-radius: 50%;
}

.drawer-content {
    padding: 0 20px 40px;
    display: flex;
    flex-direction: column;
    gap: 28px;
}

/* Control Grid (Mode, Contrast, etc) */
.control-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
}

.control-card {
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 12px;
    padding: 24px 20px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    gap: 12px;
    cursor: pointer;
    transition: all 0.2s;
    background-color: transparent;
}
/* Replicating "Paper" feel for styles with toggle inside */
.control-card:hover {
    background-color: var(--el-fill-color-lighter);
}

.control-icon {
    width: 28px;
    height: 28px;
    color: var(--el-text-color-primary);
}

.control-label {
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
}

/* Block Section */
.block-section {
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 16px;
    padding: 28px 24px;
    position: relative;
    margin-top: 8px;
}

.block-header {
    position: absolute;
    top: -12px;
    left: 20px;
    background: var(--el-text-color-primary);
    padding: 0 10px;
    border-radius: 20px;
}
:global(.dark) .block-header {
    background: var(--el-text-color-primary);
}

.block-title {
    font-size: 13px;
    font-weight: 700;
    line-height: 22px;
    color: var(--el-bg-color); 
}

/* Sub Labels */
.sub-label {
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-secondary);
    margin-bottom: 16px;
    display: block;
}

/* 
 * SHARED STYLE FOR ACTIVE CARDS (Layout, Color, Font)
 * Matches Minimals "OptionButton": Paper bg, Soft shadow, Primary icon, No border 
 */
.layout-item, .color-card, .font-card {
     border: 1px solid var(--el-border-color-lighter);
     transition: all 0.2s;
     position: relative;
     background-color: transparent;
}

.layout-item.active,
.color-card.active,
.font-card.active {
    background-color: var(--el-color-primary-light-9); /* TINTED Background */
    box-shadow: -4px 4px 12px -2px rgba(0, 0, 0, 0.12); /* Soft shadow */
    color: var(--el-text-color-primary);
    border-color: transparent; /* No border for active paper state */
}
:global(.dark) .layout-item.active,
:global(.dark) .color-card.active,
:global(.dark) .font-card.active {
     background-color: var(--el-color-primary-light-8); /* Dark mode tint adjustment if needed, or stick to opacity */
     color: var(--el-color-primary-light-3); /* Brighter text in dark mode */
     box-shadow: -20px 20px 40px -4px rgba(0, 0, 0, 0.24); 
}

/* Elements specific active tweaks */
.layout-item.active .layout-svg,
.color-card.active .color-icon {
    color: var(--el-color-primary);
}
.font-card.active {
    /* Font card specifically needs primary color text for the "Aa" or logic handled inside */
}

/* Layout Grid */
.layout-grid {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr;
    gap: 16px;
}
.layout-item {
    border-radius: 12px;
    cursor: pointer;
    overflow: hidden;
    height: 64px; 
    display: flex;
    align-items: center;
    justify-content: center;
}
.layout-item:hover {
    background-color: var(--el-fill-color-lighter);
}
.layout-svg {
    width: 100%;
    height: 100%;
    color: var(--el-text-color-secondary);
}

/* Color Grid */
.color-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
}
.color-card {
    border-radius: 12px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-secondary);
}
.color-card:hover {
    background-color: var(--el-fill-color-lighter);
}
.color-icon {
    width: 24px;
    height: 24px;
}

/* Presets */
.presets-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px; 
}
.preset-item {
    height: 56px;
    border-radius: 12px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
    border: 1px solid transparent; 
}
.preset-item:hover {
     background-color: var(--el-fill-color-lighter);
}
.preset-circle {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.preset-item.active {
    /* Background handled dynamically inline for color matching */
}
.preset-item.active .preset-circle {
    transform: scale(2); 
}

.color-visual {
   width: 24px;
   height: 24px;
   border-radius: 6px;
}
.color-visual.integrate {
   border: 2px dashed var(--el-text-color-secondary);
}
.color-visual.apparent {
   background: var(--el-text-color-secondary);
}
/* When active, color visuals change color */
.color-card.active .color-visual.integrate {
    border-color: var(--el-color-primary);
}
.color-card.active .color-visual.apparent {
    background: var(--el-color-primary);
}

/* Font Grid */
.font-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
}
.font-card {
    border-radius: 12px;
    padding: 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 10px;
    cursor: pointer;
    color: var(--el-text-color-secondary);
}
.font-card:hover {
     background-color: var(--el-fill-color-lighter);
}
.font-name {
    font-size: 13px;
    font-weight: 500;
}

/* Slider Customization */
.font-size-slider {
    padding: 0 10px;
    position: relative;
    margin-bottom: 10px;
}
.custom-slider {
    --el-slider-main-bg-color: var(--el-color-primary); 
}
:deep(.el-slider__bar) {
    background: var(--el-color-primary);
}
:deep(.el-slider__button) {
    border-color: var(--el-color-primary);
}
.slider-value {
    position: absolute;
    top: -30px; 
    left: 50%;
    transform: translateX(-50%);
    background: #212B36;
    color: #fff;
    padding: 4px 8px;
    border-radius: 6px;
    font-size: 12px;
    width: 46px;
    text-align: center;
    opacity: 0;
    transition: opacity 0.2s;
    pointer-events: none;
}
.font-size-slider:hover .slider-value {
    opacity: 1;
}
</style>
