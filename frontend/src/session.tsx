import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { api, guardarToken, hayToken } from './api'
import type { Configuracion, Usuario } from './api'

interface Sesion {
  yo: Usuario | null
  configuracion: Configuracion | null
  cargando: boolean
  esCliente: boolean       // rol principal de cliente: puede subir fotos
  puedeCrearTickets: boolean
  esPersonal: boolean      // agente, supervisor o administrador: ve todos los tickets de la empresa
  puedeGestionar: boolean  // agente o supervisor: trabaja los tickets
  esSupervisor: boolean
  esAdmin: boolean
  moduloActivo: (modulo: string) => boolean
  iniciarSesion: (email: string, password: string, empresaId: string) => Promise<void>
  registrarse: (nombre: string, email: string, password: string, empresaId: string) => Promise<void>
  cerrarSesion: () => void
  recargarConfiguracion: () => Promise<void>
}

const Contexto = createContext<Sesion | null>(null)

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [yo, setYo] = useState<Usuario | null>(null)
  const [configuracion, setConfiguracion] = useState<Configuracion | null>(null)
  const [cargando, setCargando] = useState<boolean>(hayToken())

  const cargarDatos = useCallback(async () => {
    const usuario = await api.yo()
    const config = await api.configuracion(usuario.empresaId)
    setYo(usuario)
    setConfiguracion(config)
  }, [])

  const cerrarSesion = useCallback(() => {
    guardarToken(null)
    setYo(null)
    setConfiguracion(null)
  }, [])

  // Si había un token guardado de una visita anterior, se restaura la sesión.
  useEffect(() => {
    if (!hayToken()) return
    cargarDatos()
      .catch(() => cerrarSesion())
      .finally(() => setCargando(false))
  }, [cargarDatos, cerrarSesion])

  // La API avisa cuando el token venció o dejó de ser válido.
  useEffect(() => {
    const alVencer = () => {
      setYo(null)
      setConfiguracion(null)
    }
    window.addEventListener('td-sesion-vencida', alVencer)
    return () => window.removeEventListener('td-sesion-vencida', alVencer)
  }, [])

  const iniciarSesion = useCallback(async (email: string, password: string, empresaId: string) => {
    const respuesta = await api.login(email, password, empresaId)
    guardarToken(respuesta.token)
    await cargarDatos()
  }, [cargarDatos])

  const registrarse = useCallback(async (nombre: string, email: string, password: string, empresaId: string) => {
    const respuesta = await api.registro(nombre, email, password, empresaId)
    guardarToken(respuesta.token)
    await cargarDatos()
  }, [cargarDatos])

  const recargarConfiguracion = useCallback(async () => {
    if (yo) setConfiguracion(await api.configuracion(yo.empresaId))
  }, [yo])

  const valor = useMemo<Sesion>(() => {
    const roles = yo?.roles ?? []
    const tiene = (...buscados: string[]) => buscados.some(r => roles.includes(r as never))
    return {
      yo,
      configuracion,
      cargando,
      esCliente: yo?.rol === 'ROLE_USER',
      puedeCrearTickets: tiene('ROLE_USER'),
      esPersonal: tiene('ROLE_AGENT', 'ROLE_SUPERVISOR', 'ROLE_ADMIN'),
      puedeGestionar: tiene('ROLE_AGENT', 'ROLE_SUPERVISOR'),
      esSupervisor: tiene('ROLE_SUPERVISOR'),
      esAdmin: tiene('ROLE_ADMIN'),
      moduloActivo: (modulo: string) => configuracion?.modulosHabilitados.includes(modulo) ?? false,
      iniciarSesion,
      registrarse,
      cerrarSesion,
      recargarConfiguracion,
    }
  }, [yo, configuracion, cargando, iniciarSesion, registrarse, cerrarSesion, recargarConfiguracion])

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

export function useSesion(): Sesion {
  const valor = useContext(Contexto)
  if (!valor) throw new Error('useSesion debe usarse dentro de ProveedorSesion')
  return valor
}
