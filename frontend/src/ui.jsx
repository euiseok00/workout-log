export function Icon({ name, size = 22 }) {
  const common = {
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.9,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    'aria-hidden': true,
  }
  const paths = {
    dashboard: <><rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/></>,
    workout: <><path d="M6 8v8M3 10v4M18 8v8M21 10v4M6 12h12"/></>,
    routine: <><path d="M8 6h13M8 12h13M8 18h13"/><path d="m3 6 1 1 2-2M3 12l1 1 2-2M3 18l1 1 2-2"/></>,
    exercises: <><path d="M4 19V8M10 19V4M16 19v-7M22 19V6"/></>,
    chevron: <path d="m9 18 6-6-6-6"/>,
    back: <path d="m15 18-6-6 6-6"/>,
    plus: <path d="M12 5v14M5 12h14"/>,
    search: <><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="3"/><path d="M8 3v4M16 3v4M3 10h18"/></>,
    logout: <><path d="M10 17l5-5-5-5M15 12H3"/><path d="M14 3h5a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-5"/></>,
    more: <><circle cx="12" cy="5" r="1" fill="currentColor"/><circle cx="12" cy="12" r="1" fill="currentColor"/><circle cx="12" cy="19" r="1" fill="currentColor"/></>,
    drag: <><path d="M8 6h.01M16 6h.01M8 12h.01M16 12h.01M8 18h.01M16 18h.01" strokeWidth="3"/></>,
    check: <path d="m5 12 4 4L19 6"/>,
    history: <><path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><path d="M3 3v5h5M12 7v5l3 2"/></>,
    close: <path d="M6 6l12 12M18 6 6 18"/>,
    user: <><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></>,
  }
  return <svg {...common}>{paths[name]}</svg>
}

export function Link({ to, navigate, className, children, ...props }) {
  return (
    <a
      href={to}
      className={className}
      onClick={(event) => {
        if (!event.metaKey && !event.ctrlKey && !event.shiftKey) {
          event.preventDefault()
          navigate(to)
        }
      }}
      {...props}
    >
      {children}
    </a>
  )
}

export function PageHeader({ title, eyebrow, action, back, navigate }) {
  return (
    <header className="page-header">
      <div className="page-title-wrap">
        {back && <button className="icon-button" onClick={() => navigate(back)} aria-label="뒤로 가기"><Icon name="back" /></button>}
        <div>
          {eyebrow && <p className="eyebrow">{eyebrow}</p>}
          <h1>{title}</h1>
        </div>
      </div>
      {action}
    </header>
  )
}

export function Loading() {
  return <div className="loading" role="status"><span className="spinner" />불러오는 중...</div>
}

export function EmptyState({ icon = 'workout', title, description, action }) {
  return <div className="empty-state"><span className="empty-icon"><Icon name={icon} size={26}/></span><h3>{title}</h3>{description && <p>{description}</p>}{action}</div>
}

export function Modal({ title, children, onClose, wide = false }) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className={`modal-sheet ${wide ? 'modal-wide' : ''}`} role="dialog" aria-modal="true" aria-label={title}>
        <div className="modal-handle" />
        <header className="modal-header"><h2>{title}</h2><button className="icon-button" onClick={onClose} aria-label="닫기"><Icon name="close" /></button></header>
        {children}
      </section>
    </div>
  )
}

export function ErrorMessage({ error }) {
  if (!error) return null
  return <div className="error-message" role="alert">{error.message || String(error)}</div>
}
