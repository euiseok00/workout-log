import { useLayoutEffect, useState } from 'react'
import allright from './allright/theme'
import './allright/allright-theme.css'
import defaultTheme from './default/theme'
import { ThemeContext } from './themeContext'

const STORAGE_KEY = 'workout_theme'
const themes = { [defaultTheme.id]: defaultTheme, [allright.id]: allright }
const validTheme = (id) => Object.hasOwn(themes, id) ? id : defaultTheme.id

export default function ThemeProvider({ children }) {
  const [currentTheme, setCurrentTheme] = useState(() => {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored === null && import.meta.env.DEV ? allright.id : validTheme(stored)
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
