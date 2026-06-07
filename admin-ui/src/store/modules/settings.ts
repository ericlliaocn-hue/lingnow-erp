import { defineStore } from 'pinia'
import { reactive, ref, watch } from 'vue'
import tinycolor from 'tinycolor2'

export const useSettingsStore = defineStore('settings', () => {
    const openDrawer = ref(false)

    // 从 localStorage 读取设置，如果没有则使用默认值
    // 升级配置版本以重置用户的主题缓存 (v6 -> v7)
    const savedSettings = localStorage.getItem('app-settings-v7')
    const defaultSettings = {
        themeMode: 'light', // 默认使用亮色模式
        contrast: 'default',
        direction: 'ltr',
        compactLayout: false,
        primaryColor: 'default',
        navLayout: 'vertical',
        navColor: 'apparent',
        layout: 'vertical',
        fontFamily: 'Public Sans',
        fontSize: 14,
        language: 'zh-CN' // 默认语言
    }

    const settings = reactive({
        ...defaultSettings,
        ...(savedSettings ? JSON.parse(savedSettings) : {})
    })

    // 如果 settings.language 不存在，则初始化为默认值
    if (!settings.language) {
        settings.language = defaultSettings.language
    }

    const primaryColorPresets: Record<string, string> = {
        default: '#00A76F', // green
        cyan: '#078DEE',
        purple: '#7635dc',
        blue: '#2065D1',
        orange: '#fda92d',
        red: '#FF3030'
    }

    const setDrawerOpen = (open: boolean) => {
        openDrawer.value = open
    }

    // Monitor Theme Mode
    watch(
        () => settings.themeMode,
        (val) => {
            const html = document.documentElement
            if (val === 'dark') {
                html.classList.add('dark')
                // Apply Minimals dark theme colors (rgb 24, 27, 33 -> #181B21)
                html.style.setProperty('--el-bg-color', '#181B21')
                html.style.setProperty('--el-bg-color-overlay', '#212B36') // grey[800]
                html.style.setProperty('--el-fill-color-blank', '#212B36')
                html.style.setProperty('--el-text-color-primary', '#FFFFFF')
                html.style.setProperty('--el-text-color-regular', '#919EAB') // grey[500]
            } else {
                html.classList.remove('dark')
                // Reset to Element Plus defaults (or specific light theme colors if needed)
                html.style.removeProperty('--el-bg-color')
                html.style.removeProperty('--el-bg-color-overlay')
                html.style.removeProperty('--el-fill-color-blank')
                html.style.removeProperty('--el-text-color-primary')
                html.style.removeProperty('--el-text-color-regular')
            }
            // 保存到 localStorage
            localStorage.setItem('app-settings-v7', JSON.stringify(settings))
        },
        { immediate: true }
    )

    // Monitor Primary Color
    watch(
        () => settings.primaryColor,
        (val) => {
            const color = primaryColorPresets[val || 'default'] || '#00A76F'
            updateThemeDisplay(color)
            // 保存到 localStorage
            localStorage.setItem('app-settings-v7', JSON.stringify(settings))
        },
        { immediate: true }
    )

    function updateThemeDisplay(color: string) {
        const el = document.documentElement
        // Set Element Plus primary color variables
        el.style.setProperty('--el-color-primary', color)

        // Generate light/dark shades
        for (let i = 1; i <= 9; i++) {
            el.style.setProperty(`--el-color-primary-light-${i}`, tinycolor(color).lighten(i * 10).toString())
        }
        el.style.setProperty(`--el-color-primary-dark-2`, tinycolor(color).darken(20).toString())
        
        // Generate rgba versions for backgrounds (light-8 = 16% opacity, light-9 = 8% opacity)
        const rgb = tinycolor(color).toRgb()
        el.style.setProperty('--el-color-primary-light-8', `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, 0.16)`)
        el.style.setProperty('--el-color-primary-light-9', `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, 0.08)`)
    }

    // Monitor Font Family
    watch(
        () => settings.fontFamily,
        (val) => {
            document.documentElement.style.fontFamily = `'${val}', sans-serif`
            document.body.style.fontFamily = `'${val}', sans-serif`
        },
        { immediate: true }
    )

    // Monitor Font Size
    watch(
        () => settings.fontSize,
        (val) => {
            const html = document.documentElement
            const body = document.body
            
            // 设置根字体大小，影响所有使用 rem 的元素
            html.style.fontSize = `${val}px`
            body.style.fontSize = `${val}px`
            
            // 设置 CSS 变量供其他地方使用
            html.style.setProperty('--base-font-size', `${val}px`)
            
            // 同时更新 Element Plus 的字体大小
            html.style.setProperty('--el-font-size-base', `${val}px`)
            html.style.setProperty('--el-font-size-small', `${val - 2}px`)
            html.style.setProperty('--el-font-size-large', `${val + 2}px`)
            html.style.setProperty('--el-font-size-extra-large', `${val + 4}px`)
        },
        { immediate: true }
    )

    // Monitor Contrast
    watch(
        () => settings.contrast,
        (val) => {
            const html = document.documentElement
            if (val === 'bold') {
                html.classList.add('contrast-bold')
                // 增强对比度的样式
                html.style.setProperty('--el-border-color', 'rgba(145, 158, 171, 0.32)')
                html.style.setProperty('--el-border-color-light', 'rgba(145, 158, 171, 0.24)')
                html.style.setProperty('--el-border-color-lighter', 'rgba(145, 158, 171, 0.16)')
            } else {
                html.classList.remove('contrast-bold')
                html.style.removeProperty('--el-border-color')
                html.style.removeProperty('--el-border-color-light')
                html.style.removeProperty('--el-border-color-lighter')
            }
        },
        { immediate: true }
    )

    // Monitor Direction (RTL/LTR)
    watch(
        () => settings.direction,
        (val) => {
            const html = document.documentElement
            const body = document.body
            
            html.setAttribute('dir', val)
            body.setAttribute('dir', val)
            
            if (val === 'rtl') {
                html.classList.add('rtl')
                body.classList.add('rtl')
                // RTL 特定样式调整
                body.style.direction = 'rtl'
                body.style.textAlign = 'right'
            } else {
                html.classList.remove('rtl')
                body.classList.remove('rtl')
                body.style.direction = 'ltr'
                body.style.textAlign = 'left'
            }
        },
        { immediate: true }
    )

    // Monitor Compact Layout
    watch(
        () => settings.compactLayout,
        (val) => {
            const html = document.documentElement
            if (val) {
                html.classList.add('compact-layout')
                // 紧凑布局的样式调整
                html.style.setProperty('--app-spacing', '16px')
                html.style.setProperty('--card-padding', '16px')
            } else {
                html.classList.remove('compact-layout')
                html.style.setProperty('--app-spacing', '24px')
                html.style.setProperty('--card-padding', '24px')
            }
        },
        { immediate: true }
    )

    // Monitor Nav Layout (For future Sidebar component usage)
    watch(
        () => settings.navLayout,
        (val) => {
            document.documentElement.setAttribute('data-nav-layout', val)
        },
        { immediate: true }
    )

    // Monitor Nav Color
    watch(
        () => settings.navColor,
        (val) => {
            document.documentElement.setAttribute('data-nav-color', val)
        },
        { immediate: true }
    )

    const resetSettings = () => {
        settings.themeMode = 'light'
        settings.contrast = 'default'
        settings.direction = 'ltr'
        settings.compactLayout = false
        settings.primaryColor = 'default'
        settings.navLayout = 'vertical'
        settings.navColor = 'integrate'
        settings.fontFamily = 'Public Sans'
        settings.fontSize = 16
    }

    // Monitor Language
    watch(
        () => settings.language,
        () => {
            localStorage.setItem('app-settings-v3', JSON.stringify(settings))
        },
        { immediate: true }
    )

    return {
        openDrawer,
        settings,
        primaryColorPresets,
        setDrawerOpen,
        resetSettings
    }
})
