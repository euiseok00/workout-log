import { useTheme } from './themeContext'
import './ThemeSelector.css'

export default function ThemeSelector() {
  const { currentTheme, setTheme } = useTheme()

  return (
    <label className="theme-selector">
      테마
      <select value={currentTheme} onChange={(event) => setTheme(event.target.value)}>
        <option value="default">기본</option>
        <option value="allright">박재훈</option>
      </select>
    </label>
  )
}
