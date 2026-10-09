# TicketDesk: Frontend

Aplicación web en React + TypeScript (Vite) que consume la API REST del backend.

## Requisitos
- Node.js 20 o superior
- El backend de TicketDesk corriendo (por defecto en http://localhost:8080)

## Cómo correrlo
```
npm install
npm run dev
```
Se abre en http://localhost:5173

La URL del backend se configura con la variable `VITE_API_URL` (ver `.env.example`). Para usar otra, copiar `.env.example` a `.env` y editarla.

## Pantallas
- **Login y registro**, con selector de empresa (el email es único por empresa)
- **Mis tickets** (cliente) y **Tickets** (personal de soporte, con filtros por estado, prioridad, nivel y categoría)
- **Nuevo ticket** por formulario y **por voz** (graba el audio y lo transcribe en el navegador)
- **Detalle del ticket**: recorrido de estados, tiempo objetivo (SLA), conversación, fotos e historial de cambios; acciones según el rol
- **Administración** (solo administrador): módulos y ventana de mantenimiento, categorías propias y usuarios

El menú y las acciones se adaptan al rol del usuario y a los módulos que tenga habilitados su empresa.

## Estructura
- `src/api.ts`: cliente de la API y tipos de los datos
- `src/session.tsx`: sesión, roles y módulos habilitados
- `src/pages/`: una pantalla por archivo
- `src/components/`: menú lateral y componentes visuales compartidos

## Compilar para producción
```
npm run build
```
Los archivos quedan en `dist/`.
