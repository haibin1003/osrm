import { ref, type Ref } from 'vue'

type Theme = 'light' | 'dark'

const THEME_KEY = 'osrm-theme'
const isDark: Ref<boolean> = ref(false)

export function initTheme(): void {
  const saved = localStorage.getItem(THEME_KEY) as Theme | null
  if (saved) {
    applyTheme(saved === 'dark')
  } else {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    applyTheme(prefersDark)
  }
}

function applyTheme(dark: boolean): void {
  isDark.value = dark
  const html = document.documentElement
  // Always set explicit data-theme so CSS selectors and tests can detect both states
  html.setAttribute('data-theme', dark ? 'dark' : 'light')
  if (dark) {
    html.classList.add('dark')
  } else {
    html.classList.remove('dark')
  }
  localStorage.setItem(THEME_KEY, dark ? 'dark' : 'light')
}

export function useTheme() {
  const toggleTheme = (): void => {
    applyTheme(!isDark.value)
  }

  return {
    isDark,
    toggleTheme
  }
}
