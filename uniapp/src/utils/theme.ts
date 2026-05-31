import {computed, ref} from 'vue'

export type Theme = 'light' | 'dark'

const THEME_KEY = 'app_theme'
const storedTheme = uni.getStorageSync(THEME_KEY) as string
// Fallback to light if stored theme is invalid (e.g. was 'frosted')
const initialTheme: Theme = (storedTheme === 'dark' || storedTheme === 'light') ? storedTheme : 'light'

const currentTheme = ref<Theme>(initialTheme)

export const useTheme = () => {
    const setTheme = (theme: Theme) => {
        currentTheme.value = theme
        uni.setStorageSync(THEME_KEY, theme)
    }

    const themeClass = computed(() => {
        return `theme-${currentTheme.value}`
    })

    const isDark = computed(() => currentTheme.value === 'dark')
    const isLight = computed(() => currentTheme.value === 'light')

    // Helper to get specific colors based on theme (optional, can also use CSS vars)
    const themeColors = computed(() => {
        switch (currentTheme.value) {
            case 'dark':
                return {
                    bg: '#1a1a1a',
                    cardBg: '#2c2c2c',
                    text: '#ffffff',
                    subText: '#a0a0a0',
                    border: '#333333',
                    navBg: '#1a1a1a',
                    navText: '#ffffff'
                }
            default: // light
                return {
                    bg: '#ffffff',
                    cardBg: '#ffffff',
                    text: '#333333',
                    subText: '#999999',
                    border: '#f5f5f5',
                    navBg: '#ffffff',
                    navText: '#000000'
                }
        }
    })

    const updateNavigationBar = () => {
        const colors = themeColors.value
        uni.setNavigationBarColor({
            frontColor: colors.navText,
            backgroundColor: colors.navBg,
            animation: {
                duration: 300,
                timingFunc: 'easeIn'
            }
        })
    }

    return {
        currentTheme,
        setTheme,
        themeClass,
        themeColors,
        isDark,
        isLight,
        updateNavigationBar
    }
}
