import { Navigate, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useSesion } from './session'
import Layout from './components/Layout'
import Login from './pages/Login'
import Tickets from './pages/Tickets'
import NuevoTicket from './pages/NuevoTicket'
import NuevoTicketVoz from './pages/NuevoTicketVoz'
import TicketDetalle from './pages/TicketDetalle'
import Admin from './pages/Admin'

function Protegida({ children }: { children: ReactNode }) {
  const { yo, cargando } = useSesion()
  if (cargando) return <p className="cargando">Cargando…</p>
  if (!yo) return <Navigate to="/login" replace />
  return <>{children}</>
}

function SoloAdmin({ children }: { children: ReactNode }) {
  const { esAdmin } = useSesion()
  return esAdmin ? <>{children}</> : <Navigate to="/" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<Protegida><Layout /></Protegida>}>
        <Route path="/" element={<Tickets />} />
        <Route path="/tickets/nuevo" element={<NuevoTicket />} />
        <Route path="/tickets/nuevo-voz" element={<NuevoTicketVoz />} />
        <Route path="/tickets/:id" element={<TicketDetalle />} />
        <Route path="/admin" element={<SoloAdmin><Admin /></SoloAdmin>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
