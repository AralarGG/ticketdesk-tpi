import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useSesion } from '../session'

const ETIQUETA_ROL: Record<string, string> = {
  ROLE_USER: 'Cliente',
  ROLE_AGENT: 'Agente',
  ROLE_SUPERVISOR: 'Supervisor',
  ROLE_ADMIN: 'Administrador',
}

export default function Layout() {
  const { yo, esPersonal, puedeCrearTickets, esAdmin, moduloActivo, cerrarSesion } = useSesion()
  const navegar = useNavigate()

  function salir() {
    cerrarSesion()
    navegar('/login')
  }

  return (
    <div className="app">
      <aside className="riel">
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
