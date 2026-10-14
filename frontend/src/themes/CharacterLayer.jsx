import { useEffect } from 'react'
import { themes } from './index'
import { useCharacterEvent } from './characterEventContext'
import { useTheme } from './themeContext'
import './CharacterLayer.css'

export default function CharacterLayer() {
  const { currentTheme } = useTheme()
  const { activeEvent, clearCharacter } = useCharacterEvent()

  useEffect(() => {
    if (!activeEvent) return
    const timer = window.setTimeout(clearCharacter, 4000)
    return () => window.clearTimeout(timer)
  }, [activeEvent, clearCharacter])

  if (!activeEvent) return null

  const theme = themes[currentTheme]
  const image = theme.images?.[activeEvent.type]
  if (!image) return null
  const line = theme.messages[activeEvent.type]
  const message = Array.isArray(line) ? line[(activeEvent.id - 1) % line.length] : line

  return (
    <div key={activeEvent.id} className="character-layer" data-event={activeEvent.type} role={message ? 'status' : undefined}>
      {message && <p className="character-message">{message}</p>}
      <img src={image} alt="" className="character-image"/>
    </div>
  )
}
