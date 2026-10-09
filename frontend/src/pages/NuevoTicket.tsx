import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { api, ApiError } from '../api'
import type { Categoria, Prioridad } from '../api'
import { useSesion } from '../session'
import { Aviso, ETIQUETA_PRIORIDAD, PRIORIDADES } from '../components/ui'

export default function NuevoTicket() {
  const { puedeCrearTickets } = useSesion()
  const navegar = useNavigate()

  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [titulo, setTitulo] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [categoriaId, setCategoriaId] = useState('')
  const [prioridad, setPrioridad] = useState<Prioridad>('MEDIA')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    api.categorias().then(setCategorias).catch(e => setError(e instanceof ApiError ? e.message : 'No se pudieron cargar las categorías.'))
  }, [])

  if (!puedeCrearTickets) return <Navigate to="/" replace />

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      const ticket = await api.crearTicket({ titulo, descripcion, categoriaId, prioridad })
      navegar(`/tickets/${ticket.id}`)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo crear el ticket.')
      setEnviando(false)
    }
  }

  return (
    <>
      <header className="cabecera">
        <div>
          <h1>Nuevo ticket</h1>
          <p className="subtitulo">Contanos qué pasó y lo derivamos a quien pueda ayudarte.</p>
        </div>
      </header>

      <form className="panel formulario" onSubmit={enviar}>
        {error && <Aviso tipo="error">{error}</Aviso>}

        <label>
          Título
          <input value={titulo} onChange={e => setTitulo(e.target.value)} maxLength={200} required />
        </label>

        <label>
          Categoría
          <select value={categoriaId} onChange={e => setCategoriaId(e.target.value)} required>
            <option value="">Elegí una categoría</option>
            {categorias.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
          </select>
        </label>

        <fieldset>
          <legend>Qué tan urgente es para vos</legend>
          <div className="opciones">
            {PRIORIDADES.map(p => (
              <label key={p} className="casilla">
                <input type="radio" name="prioridad" checked={prioridad === p} onChange={() => setPrioridad(p)} />
                {ETIQUETA_PRIORIDAD[p]}
              </label>
            ))}
          </div>
        </fieldset>

        <label>
          Descripción
          <textarea value={descripcion} onChange={e => setDescripcion(e.target.value)} rows={6} required />
        </label>

        <div className="acciones">
          <button type="submit" className="boton" disabled={enviando}>{enviando ? 'Creando…' : 'Crear ticket'}</button>
          <Link to="/" className="boton boton-secundario">Cancelar</Link>
        </div>
        <p className="ayuda">Podés agregar capturas de pantalla una vez creado el ticket.</p>
      </form>
    </>
  )
}
