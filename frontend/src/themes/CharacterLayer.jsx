import { useEffect } from 'react'
import allright from './allright/theme'
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

  if (currentTheme !== allright.id || !activeEvent) return null

  const image = allright.images[activeEvent.type]
  if (!image) return null
  const line = allright.messages[activeEvent.type]
  const message = Array.isArray(line) ? line[(activeEvent.id - 1) % line.length] : line

  return (
    <div key={activeEvent.id} className="character-layer" data-event={activeEvent.type} role={message ? 'status' : undefined}>
      {message && <p className="character-message">{message}</p>}
      <img src={image} alt="" className="character-image"/>
    </div>
  )
}
