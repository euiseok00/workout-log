import { cloneElement, useEffect, useState } from 'react'

export default function TransitionPresence({ children }) {
  const [retained, setRetained] = useState(children)

  useEffect(() => {
    if (children) {
      // Keep the last rendered content only for its exit animation.
      // oxlint-disable-next-line react/set-state-in-effect
      setRetained(children)
      return
    }
    const delay = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 140
    const timer = setTimeout(() => setRetained(null), delay)
    return () => clearTimeout(timer)
  }, [children])

  const content = children || retained
  return content ? cloneElement(content, {
    'data-exiting': children ? undefined : true,
    inert: children ? undefined : true,
    'aria-hidden': children ? undefined : true,
  }) : null
}
