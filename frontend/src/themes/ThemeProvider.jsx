import { useLayoutEffect, useState } from 'react'
import './allright/allright-theme.css'
import './apdo/apdo-theme.css'
import { themes } from './index'
import { ThemeContext } from './themeContext'

const STORAGE_KEY = 'workout_theme'
const validTheme = (id) => Object.hasOwn(themes, id) ? id : 'default'

export default function ThemeProvider({ children }) {
  const [currentTheme, setCurrentTheme] = useState(() => {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored === null && import.meta.env.DEV ? 'allright' : validTheme(stored)
  })

  useLayoutEffect(() => {
    document.documentElement.dataset.theme = currentTheme
  }, [currentTheme])

  const setTheme = (id) => {
    const nextTheme = validTheme(id)
    localStorage.setItem(STORAGE_KEY, nextTheme)
    setCurrentTheme(nextTheme)
  }

  return <ThemeContext.Provider value={{ currentTheme, setTheme }}>{children}</ThemeContext.Provider>
}
