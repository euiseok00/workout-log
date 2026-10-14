import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import ThemeProvider from './themes/ThemeProvider.jsx'
import CharacterEventProvider from './themes/CharacterEventProvider.jsx'
import CharacterLayer from './themes/CharacterLayer.jsx'
import ThemeSelector from './themes/ThemeSelector.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ThemeProvider>
      <CharacterEventProvider>
        <App />
        <CharacterLayer />
        <ThemeSelector />
      </CharacterEventProvider>
    </ThemeProvider>
  </StrictMode>,
)
