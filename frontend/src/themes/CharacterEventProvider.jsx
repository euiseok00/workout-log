import { useCallback, useRef, useState } from 'react'
import { CharacterEventContext } from './characterEventContext'

export default function CharacterEventProvider({ children }) {
  const [activeEvent, setActiveEvent] = useState(null)
  const nextId = useRef(0)
  const triggerCharacter = useCallback((eventType, payload) => {
    setActiveEvent({ id: ++nextId.current, type: eventType, payload })
  }, [])
  const clearCharacter = useCallback(() => setActiveEvent(null), [])

  return (
    <CharacterEventContext.Provider value={{ activeEvent, triggerCharacter, clearCharacter }}>
      {children}
    </CharacterEventContext.Provider>
  )
}
