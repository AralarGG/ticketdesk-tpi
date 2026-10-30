import { useEffect } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useSesion } from '../session'
import type { Rol } from '../api'

const ETIQUETA_ROL: Record<string, string> = {
  ROLE_USER: 'Cliente',
  ROLE_AGENT: 'Agente',
  ROLE_SUPERVISOR: 'Supervisor',
  ROLE_ADMIN: 'Administrador',
}

// Cada rol tiene su propio icono, para reconocerlo de un vistazo ademas del color
function IconoRol({ rol }: { rol: Rol }) {
  const propiedades = {
    width: 22,
    height: 22,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.8,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    'aria-hidden': true,
  }
  switch (rol) {
    case 'ROLE_AGENT':
      return (
        <svg {...propiedades}>
          <path d="M4 14v-2a8 8 0 0 1 16 0v2" />
          <rect x="3" y="14" width="4" height="6" rx="1.5" />
          <rect x="17" y="14" width="4" height="6" rx="1.5" />
          <path d="M19 20c0 1.5-2 2-5 2" />
        </svg>
      )
    case 'ROLE_SUPERVISOR':
      return (
        <svg {...propiedades}>
          <path d="M12 3l8 3v5c0 5-3.4 8.5-8 10-4.6-1.5-8-5-8-10V6l8-3z" />
          <path d="M8.5 12l2.5 2.5 4.5-5" />
        </svg>
      )
    case 'ROLE_ADMIN':
      return (
        <svg {...propiedades}>
          <circle cx="12" cy="12" r="3" />
          <path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M19.1 4.9L17 7M7 17l-2.1 2.1" />
        </svg>
      )
    default:
      return (
        <svg {...propiedades}>
          <circle cx="12" cy="8" r="4" />
          <path d="M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7" />
        </svg>
      )
  }
}

export default function Layout() {
  const { yo, esPersonal, puedeCrearTickets, esAdmin, moduloActivo, cerrarSesion } = useSesion()
  const navegar = useNavigate()
  const rolPrincipal: Rol = yo?.rol ?? 'ROLE_USER'
  const nombreRol = ETIQUETA_ROL[rolPrincipal] ?? rolPrincipal

  // El titulo de la pestana indica el rol, util cuando hay varias ventanas abiertas
  useEffect(() => {
    document.title = `TicketDesk - ${nombreRol}`
    return () => {
      document.title = 'TicketDesk'
    }
  }, [nombreRol])

  function salir() {
    cerrarSesion()
    navegar('/login')
  }

  return (
    <div className="app" data-rol={rolPrincipal}>
      <aside className="riel">
        <div className="rol-banda" role="status" aria-label={`Sesión iniciada como ${nombreRol}`}>
          <IconoRol rol={rolPrincipal} />
          <span className="rol-banda-texto">
            <small>Sesión como</small>
            <strong>{nombreRol}</strong>
          </span>
        </div>

        <div className="marca">
          <span className="marca-nombre">TicketDesk</span>
          <span className="marca-empresa">{yo?.empresaNombre}</span>
        </div>

        <nav className="menu" aria-label="Principal">
          <NavLink to="/" end>{esPersonal ? 'Tickets' : 'Mis tickets'}</NavLink>
          {puedeCrearTickets && <NavLink to="/tickets/nuevo">Nuevo ticket</NavLink>}
          {puedeCrearTickets && moduloActivo('voz') && <NavLink to="/tickets/nuevo-voz">Nuevo ticket por voz</NavLink>}
          {esAdmin && <NavLink to="/admin">Administración</NavLink>}
        </nav>

        <div className="usuario">
          <strong>{yo?.nombre}</strong>
          <span>{(yo?.roles ?? []).map(r => ETIQUETA_ROL[r] ?? r).join(' y ')}</span>
          <button type="button" className="boton-texto" onClick={salir}>Cerrar sesión</button>
        </div>
      </aside>

      <main className="contenido">
        <Outlet />
      </main>
    </div>
  )
}
