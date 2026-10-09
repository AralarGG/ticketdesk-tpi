import { useEffect, useRef, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { api, ApiError } from '../api'
import type { Categoria, Prioridad } from '../api'
import { useSesion } from '../session'
import { Aviso, ETIQUETA_PRIORIDAD, PRIORIDADES } from '../components/ui'

type Fase = 'inicial' | 'grabando' | 'revision'

// La API de reconocimiento de voz no está tipada en TypeScript, por eso se accede sin tipos.
const ReconocimientoVoz = (window as any).SpeechRecognition ?? (window as any).webkitSpeechRecognition

export default function NuevoTicketVoz() {
  const { puedeCrearTickets, moduloActivo } = useSesion()
  const navegar = useNavigate()

  const [fase, setFase] = useState<Fase>('inicial')
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [texto, setTexto] = useState('')
  const [audio, setAudio] = useState<Blob | null>(null)
  const [audioUrl, setAudioUrl] = useState<string | null>(null)
  const [categoriaId, setCategoriaId] = useState('')
  const [tituloSugerido, setTituloSugerido] = useState('')
  const [prioridad, setPrioridad] = useState<Prioridad>('MEDIA')
  const [error, setError] = useState<string | null>(null)
  const [sugiriendo, setSugiriendo] = useState(false)
  const [enviando, setEnviando] = useState(false)

  const textoRef = useRef('')
  const reconocimiento = useRef<any>(null)
  const grabador = useRef<MediaRecorder | null>(null)
  const partes = useRef<Blob[]>([])
  const microfono = useRef<MediaStream | null>(null)

  useEffect(() => {
    api.categorias().then(setCategorias).catch(() => setCategorias([]))
  }, [])

  // Al soltar la pantalla se corta cualquier grabación en curso.
  useEffect(() => () => {
    reconocimiento.current?.stop()
    if (grabador.current?.state === 'recording') grabador.current.stop()
    microfono.current?.getTracks().forEach(t => t.stop())
  }, [])

  useEffect(() => () => { if (audioUrl) URL.revokeObjectURL(audioUrl) }, [audioUrl])

  if (!puedeCrearTickets || !moduloActivo('voz')) return <Navigate to="/" replace />

  function actualizarTexto(nuevo: string) {
    textoRef.current = nuevo
    setTexto(nuevo)
  }

  async function iniciar() {
    setError(null)
    setAudio(null)
    setAudioUrl(null)
    actualizarTexto('')
    try {
      const flujo = await navigator.mediaDevices.getUserMedia({ audio: true })
      microfono.current = flujo

      const mime = MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : ''
      const mr = new MediaRecorder(flujo, mime ? { mimeType: mime } : undefined)
      partes.current = []
      mr.ondataavailable = e => { if (e.data.size > 0) partes.current.push(e.data) }
      mr.onstop = () => {
        const blob = new Blob(partes.current, { type: mr.mimeType || 'audio/webm' })
        setAudio(blob)
        setAudioUrl(URL.createObjectURL(blob))
        flujo.getTracks().forEach(t => t.stop())
      }
      mr.start()
      grabador.current = mr

      if (ReconocimientoVoz) {
        const r = new ReconocimientoVoz()
        r.lang = 'es-AR'
        r.continuous = true
        r.interimResults = true
        r.onresult = (ev: any) => {
          let completo = ''
          let parcial = ''
          for (let i = 0; i < ev.results.length; i++) {
            const fragmento: string = ev.results[i][0].transcript
            if (ev.results[i].isFinal) completo += fragmento + ' '
            else parcial += fragmento
          }
          actualizarTexto((completo + parcial).trim())
        }
        r.onerror = () => setError('No se pudo transcribir lo que dijiste. Podés escribir el texto a mano.')
        r.start()
        reconocimiento.current = r
      }
      setFase('grabando')
    } catch {
      setError('No se pudo acceder al micrófono. Revisá el permiso del navegador para este sitio.')
    }
  }

  function detener() {
    reconocimiento.current?.stop()
    if (grabador.current?.state === 'recording') grabador.current.stop()
    setFase('revision')
    // La transcripción final puede llegar unos instantes después de detener la grabación.
    window.setTimeout(() => sugerir(textoRef.current), 800)
  }

  async function sugerir(transcripcion: string) {
    if (!transcripcion.trim()) return
    setSugiriendo(true)
    try {
      const sugerencia = await api.sugerirVoz(transcripcion)
      setTituloSugerido(sugerencia.tituloSugerido)
      if (sugerencia.categoriaId) setCategoriaId(sugerencia.categoriaId)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo sugerir una categoría.')
    } finally {
      setSugiriendo(false)
    }
  }

  async function crear() {
    if (!audio) {
      setError('Falta el audio. Volvé a grabar el mensaje.')
      return
    }
    setError(null)
    setEnviando(true)
    try {
      const ticket = await api.crearTicketVoz({ audio, transcripcion: texto, categoriaId, prioridad })
      navegar(`/tickets/${ticket.id}`)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo crear el ticket.')
      setEnviando(false)
    }
  }

  function grabarDeNuevo() {
    setFase('inicial')
    setTituloSugerido('')
    setCategoriaId('')
    actualizarTexto('')
  }

  return (
    <>
      <header className="cabecera">
        <div>
          <h1>Nuevo ticket por voz</h1>
          <p className="subtitulo">Contá el problema con tus palabras. Vas a poder revisar el texto antes de enviarlo.</p>
        </div>
      </header>

      {error && <Aviso tipo="error">{error}</Aviso>}
      {!ReconocimientoVoz && (
        <Aviso tipo="info">
          Este navegador no transcribe voz automáticamente. Podés grabar el audio y escribir el texto a mano
          (Chrome o Edge lo transcriben solos).
        </Aviso>
      )}

      <section className="panel voz">
        {fase === 'inicial' && (
          <div className="voz-centro">
            <button type="button" className="boton-grabar" onClick={iniciar}>Empezar a grabar</button>
            <p className="ayuda">El navegador te va a pedir permiso para usar el micrófono.</p>
          </div>
        )}

        {fase === 'grabando' && (
          <div className="voz-centro">
            <div className="grabando" role="status"><span className="latido" aria-hidden="true" /> Grabando</div>
            <p className="transcripcion-viva">{texto || 'Empezá a hablar…'}</p>
            <button type="button" className="boton" onClick={detener}>Terminar</button>
          </div>
        )}

        {fase === 'revision' && (
          <div className="formulario">
            {audioUrl && (
              <label>
                Audio grabado
                <audio controls src={audioUrl} />
              </label>
            )}

            <label>
              Lo que dijiste (podés corregirlo)
              <textarea
                value={texto}
                onChange={e => actualizarTexto(e.target.value)}
                onBlur={() => sugerir(texto)}
                rows={5}
                required
              />
            </label>

            {tituloSugerido && <p className="ayuda">Título del ticket: <strong>{tituloSugerido}</strong></p>}

            <label>
              Categoría {sugiriendo && <small>(buscando sugerencia…)</small>}
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

            <div className="acciones">
              <button type="button" className="boton" onClick={crear} disabled={enviando || !texto.trim() || !categoriaId}>
                {enviando ? 'Creando…' : 'Crear ticket'}
              </button>
              <button type="button" className="boton boton-secundario" onClick={grabarDeNuevo}>Grabar de nuevo</button>
              <Link to="/" className="boton-texto">Cancelar</Link>
            </div>
          </div>
        )}
      </section>
    </>
  )
}
