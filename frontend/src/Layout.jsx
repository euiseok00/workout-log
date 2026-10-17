import { clearAuth } from './api'
import { useCharacterEvent } from './themes/characterEventContext'
import { CHARACTER_EVENTS } from './themes/characterEvents'
import { Icon, Link } from './ui'

const NAV_ITEMS = [
  ['/dashboard', 'dashboard', '대시보드'],
  ['/workout', 'workout', '운동 시작'],
  ['/routines', 'routine', '루틴'],
  ['/exercises', 'exercises', '운동 종목'],
]

export default function Layout({ path, navigate, auth, onLogout, children }) {
  const { triggerCharacter } = useCharacterEvent()
  const draftKey = `workout_draft:${auth.user.id}`
  const hasDraft = Boolean(localStorage.getItem(draftKey))
  const openWorkout = (to) => { triggerCharacter(CHARACTER_EVENTS.WORKOUT_ENTER); navigate(to) }
  const navNavigation = {
    '/workout': openWorkout,
    '/routines': (to) => { triggerCharacter(CHARACTER_EVENTS.ROUTINES_ENTER); navigate(to) },
    '/exercises': (to) => { triggerCharacter(CHARACTER_EVENTS.EXERCISES_ENTER); navigate(to) },
  }
  const logout = () => {
    clearAuth()
    onLogout()
    navigate('/login', true)
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Link to="/dashboard" navigate={navigate} className="brand">
          <span className="brand-mark"><Icon name="workout" size={24}/></span>
          <span>Workout<br/><b>Log</b></span>
        </Link>
        <nav className="side-nav" aria-label="주요 메뉴">
          {NAV_ITEMS.map(([to, icon, label]) => {
            const active = path === to || (to !== '/dashboard' && path.startsWith(`${to}/`))
            return <Link key={to} to={to} navigate={navNavigation[to] || navigate} className={`nav-item ${active ? 'active' : ''}`}><span className="nav-icon"><Icon name={icon}/>{to === '/workout' && hasDraft && <i className="draft-dot"/>}</span>{label}</Link>
          })}
        </nav>
        <div className="sidebar-user"><span className="avatar"><Icon name="user" size={18}/></span><div><b>{auth.user.loginId}</b><small>내 운동 기록</small></div><button onClick={logout} className="icon-button" aria-label="로그아웃"><Icon name="logout" size={20}/></button></div>
      </aside>

      <main className="main-content">{children}</main>

      <nav className="bottom-nav" aria-label="주요 메뉴">
        {NAV_ITEMS.map(([to, icon, label]) => {
          const active = path === to || (to !== '/dashboard' && path.startsWith(`${to}/`))
          return <Link key={to} to={to} navigate={navNavigation[to] || navigate} className={`bottom-nav-item ${active ? 'active' : ''}`}><span className="nav-icon"><Icon name={icon}/>{to === '/workout' && hasDraft && <i className="draft-dot"/>}</span><small>{label}</small></Link>
        })}
      </nav>
    </div>
  )
}
