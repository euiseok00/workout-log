import { createContext, useContext } from 'react'

export const CharacterEventContext = createContext(null)

export function useCharacterEvent() {
  const context = useContext(CharacterEventContext)
  if (!context) throw new Error('useCharacterEvent must be used within CharacterEventProvider')
  return context
}
