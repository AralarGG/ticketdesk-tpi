# TicketDesk: Diagramas UML

## Documento base: Trabajo Final Integrador UTN TUPaD

Este documento reúne los diagramas que complementan la documentación del sistema: casos de uso, componentes, actividades, secuencia, despliegue y entidad-relación. Están escritos en un formato de texto que GitHub dibuja automáticamente, de modo que se mantienen junto al código y se pueden editar sin herramientas externas.

Los nombres de actores, estados y entidades siguen el `diccionario-nomenclatura-ticketdesk.md`. Los casos de uso se detallan en `casos-de-uso-ticketdesk.md`.

---

## 1. Diagrama de casos de uso

Muestra qué actor realiza cada caso de uso. El supervisor incluye todo lo que hace un agente. Los casos automáticos los ejecuta el sistema. El formato de texto no tiene una figura propia para los casos de uso, por lo que se representan con óvalos agrupados por área.

```mermaid
flowchart LR
    V(["Visitante"])
    C(["Cliente"])
    A(["Agente"])
    S(["Supervisor"])
    D(["Administrador"])
    SI(["Sistema"])

    subgraph CUENTA ["Cuenta"]
        UC01(["UC-01 Iniciar sesión"])
        UC02(["UC-02 Registrarse como cliente"])
    end

    subgraph CLIENTE ["Tickets del cliente"]
        UC03(["UC-03 Crear ticket por formulario"])
        UC04(["UC-04 Crear ticket por voz"])
        UC05(["UC-05 Consultar mis tickets"])
        UC06(["UC-06 Comentar un ticket"])
        UC07(["UC-07 Adjuntar una foto"])
        UC08(["UC-08 Confirmar el cierre o reabrir"])
    end

    subgraph SOPORTE ["Gestión de tickets"]
        UC09(["UC-09 Listar y filtrar tickets"])
        UC10(["UC-10 Tomar un ticket"])
        UC11(["UC-11 Asignar a otra persona"])
        UC12(["UC-12 Trabajar un ticket"])
        UC13(["UC-13 Escalar a un supervisor"])
        UC14(["UC-14 Cambiar el nivel de atención"])
        UC15(["UC-15 Consultar el historial"])
    end

    subgraph ADMIN ["Administración"]
        UC16(["UC-16 Configurar módulos y mantenimiento"])
        UC17(["UC-17 Gestionar categorías propias"])
        UC18(["UC-18 Gestionar usuarios y roles"])
    end

    subgraph AUTO ["Reglas automáticas"]
        UC19(["UC-19 Cerrar un ticket resuelto"])
        UC20(["UC-20 Devolver a Nivel 1 desde Crítico"])
        UC21(["UC-21 Aplicar la ventana de mantenimiento"])
    end

    V --- UC01
    V --- UC02

    C --- UC01
    C --- UC03
    C --- UC04
    C --- UC05
    C --- UC06
    C --- UC07
    C --- UC08

    A --- UC01
    A --- UC06
    A --- UC09
    A --- UC10
    A --- UC12
    A --- UC13
    A --- UC14
    A --- UC15

    S -.->|"incluye las funciones del agente"| A
    S --- UC11

    D --- UC01
    D --- UC06
    D --- UC09
    D --- UC15
    D --- UC16
    D --- UC17
    D --- UC18

    SI --- UC19
    SI --- UC20
    SI --- UC21
```

## 2. Diagrama de componentes

Muestra las partes que forman el sistema y cómo se comunican. El backend sigue una arquitectura por capas: la seguridad filtra cada petición, los controladores la reciben, los servicios aplican las reglas de negocio y los repositorios acceden a los datos.

```mermaid
flowchart TB
    subgraph NAVEGADOR ["Navegador del usuario"]
        subgraph FRONT ["Frontend React y TypeScript"]
            PAG["Páginas: login, tickets, nuevo ticket, nuevo ticket por voz, detalle y administración"]
            SES["Sesión, roles y módulos habilitados"]
            CLI["Cliente de la API"]
            PAG --> SES
            PAG --> CLI
        end
        VOZ["Grabación y reconocimiento de voz del navegador"]
        PAG --> VOZ
    end

    subgraph BACK ["Backend Spring Boot"]
        SEG["Seguridad: filtro JWT, control de roles y ventana de mantenimiento"]
        CTRL["Controladores REST"]
        SRV["Servicios: tickets, comunicación, voz, usuarios, categorías y configuración"]
        REP["Repositorios JPA"]
        PLAN["Tareas programadas: cierre automático y rebote desde Crítico"]
        SEG --> CTRL
        CTRL --> SRV
        SRV --> REP
        PLAN --> SRV
    end

    BD[("Base de datos PostgreSQL")]
    ARCH["Cloudinary: fotos y audios"]

    CLI -->|"HTTPS con token JWT"| SEG
    REP -->|"JDBC"| BD
    SRV -->|"subida de archivos"| ARCH
    PAG -.->|"muestra fotos y audios"| ARCH
```

## 3. Diagramas de actividades

### 3.1 Ciclo de vida de un ticket

```mermaid
flowchart TD
    INICIO(["Inicio"]) --> A["El cliente crea el ticket: estado NUEVO y nivel NIVEL_1"]
    A --> B["Un agente lo toma o un supervisor lo asigna: estado ASIGNADO"]
    B --> C["El agente empieza a trabajar: estado EN_PROGRESO"]
    C --> D{"¿Necesita información del cliente?"}
    D -->|"Sí"| E["Estado ESPERANDO_CLIENTE"]
    E -->|"El cliente responde"| C
    D -->|"No"| F{"¿Requiere a un supervisor?"}
    F -->|"Sí"| G["Se escala con motivo: estado ESCALADO, sube un nivel y se reasigna a un supervisor"]
    G --> H{"¿El supervisor lo resuelve?"}
    H -->|"No, lo retoma"| C
    H -->|"Sí"| J
    F -->|"No"| J["El agente lo marca como RESUELTO"]
    J --> K{"¿Qué ocurre en las siguientes 72 horas?"}
    K -->|"El cliente confirma"| L["Estado CERRADO por el cliente"]
    K -->|"El cliente indica que continúa"| M["Estado REABIERTO"]
    M -->|"El agente lo retoma"| C
    K -->|"El cliente no responde"| N["Estado CERRADO automáticamente por el sistema"]
    L --> FIN(["Fin"])
    N --> FIN
```

### 3.2 Creación de un ticket por voz

```mermaid
flowchart TD
    subgraph CLIENTE ["Cliente"]
        A1(["Inicio"]) --> A2["Elige Nuevo ticket por voz"]
        A2 --> A3["Toca Empezar a grabar y cuenta el problema"]
        A3 --> A4["Toca Terminar"]
        A6["Revisa el audio, corrige el texto y elige categoría y prioridad"]
        A7["Confirma con Crear ticket"]
        A6 --> A7
    end

    subgraph NAVEGADOR ["Navegador"]
        B1["Graba el audio y transcribe en vivo"]
        B2["Envía el texto para pedir una sugerencia"]
        B1 --> B2
    end

    subgraph SISTEMA ["Sistema"]
        C1["Busca palabras clave y devuelve categoría y título sugeridos, o Consulta si no hay coincidencias"]
        C2{"¿El módulo de voz está habilitado?"}
        C3["Sube el audio y crea el ticket con canal VOZ, audio y transcripción"]
        C4["Rechaza con 403"]
        C2 -->|"Sí"| C3
        C2 -->|"No"| C4
    end

    A3 --> B1
    A4 --> B2
    B2 --> C1
    C1 --> A6
    A7 --> C2
    C3 --> FIN(["Fin: ticket NUEVO"])
    C4 --> FIN2(["Fin sin ticket"])
```

## 4. Diagrama de secuencia: creación de un ticket por voz

```mermaid
sequenceDiagram
    actor C as Cliente
    participant N as Navegador
    participant B as Backend
    participant A as Cloudinary
    participant D as PostgreSQL

    C->>N: Toca Empezar a grabar
    N->>N: Graba el audio y transcribe en vivo
    C->>N: Toca Terminar
    N->>B: POST /tickets/voz/sugerencia con la transcripción
    B-->>N: Categoría y título sugeridos
    C->>N: Revisa, corrige y confirma
    N->>B: POST /tickets/voz con audio, texto, categoría y prioridad
    B->>B: Verifica el rol de cliente y el módulo de voz
    B->>A: Sube el audio
    A-->>B: Dirección del audio
    B->>D: Guarda el ticket con canal VOZ, audio y transcripción
    D-->>B: Ticket guardado
    B-->>N: Ticket creado en estado NUEVO
    N-->>C: Muestra el detalle del ticket
```

## 5. Diagrama de despliegue

Muestra dónde se ejecuta cada parte del sistema en el entorno en línea.

```mermaid
flowchart LR
    U["Navegador del usuario"]

    subgraph GITHUB ["GitHub"]
        REPO["Repositorio ticketdesk-tpi"]
    end

    subgraph VERCEL ["Vercel, plan gratuito"]
        FRONT["Frontend estático React"]
    end

    subgraph RENDER ["Render, plan gratuito"]
        BACK["Contenedor Docker con el backend Spring Boot"]
    end

    subgraph SUPABASE ["Supabase, plan gratuito"]
        BD[("PostgreSQL")]
    end

    ARCH["Cloudinary"]

    U -->|"HTTPS"| FRONT
    U -->|"HTTPS: API REST con token JWT"| BACK
    U -.->|"HTTPS: fotos y audios"| ARCH
    BACK -->|"JDBC con SSL por el Session pooler, puerto 5432"| BD
    BACK -->|"HTTPS"| ARCH
    REPO -->|"despliegue automático en cada commit"| FRONT
    REPO -->|"despliegue automático en cada commit"| BACK
```

Particularidades del plan gratuito, descritas en `despliegue-ticketdesk.md`: el backend se suspende tras 15 minutos sin visitas y la base de datos se pausa tras una semana sin actividad.

## 6. Diagrama entidad-relación

Resume las nueve tablas del modelo de datos con sus claves. El detalle de todos los campos está en `DER-ticketdesk.md` y en `schema.sql`. El campo `entidad_id` del historial apunta a distintas tablas según el valor de `entidad`, por eso no tiene una clave foránea.

```mermaid
erDiagram
    EMPRESAS ||--|| CONFIGURACION_EMPRESA : "tiene"
    EMPRESAS ||--o{ USUARIOS : "agrupa"
    EMPRESAS |o--o{ CATEGORIAS : "define las propias"
    EMPRESAS ||--o{ TICKETS : "recibe"
    USUARIOS ||--o{ USUARIO_ROLES : "tiene roles"
    USUARIOS ||--o{ TICKETS : "crea como cliente"
    USUARIOS |o--o{ TICKETS : "atiende como agente"
    USUARIOS ||--o{ COMENTARIOS : "escribe"
    USUARIOS |o--o{ HISTORIAL_CAMBIOS : "realiza"
    CATEGORIAS ||--o{ TICKETS : "clasifica"
    TICKETS ||--o{ COMENTARIOS : "contiene"
    TICKETS ||--o{ ADJUNTOS : "contiene"
    COMENTARIOS |o--o{ ADJUNTOS : "puede incluir"

    EMPRESAS {
        uuid id PK
        string nombre
        boolean activa
    }
    CONFIGURACION_EMPRESA {
        uuid id PK
        uuid empresa_id FK
        jsonb modulos_habilitados
    }
    USUARIOS {
        uuid id PK
        uuid empresa_id FK
        string email
        string rol
        boolean activo
    }
    USUARIO_ROLES {
        uuid id PK
        uuid usuario_id FK
        string rol
    }
    CATEGORIAS {
        uuid id PK
        uuid empresa_id FK
        string nombre
    }
    TICKETS {
        uuid id PK
        uuid empresa_id FK
        uuid usuario_id FK
        uuid agente_id FK
        uuid categoria_id FK
        string estado
        string nivel_atencion
        string canal_origen
    }
    COMENTARIOS {
        uuid id PK
        uuid ticket_id FK
        uuid usuario_id FK
        string contenido
    }
    ADJUNTOS {
        uuid id PK
        uuid ticket_id FK
        uuid comentario_id FK
        string url
    }
    HISTORIAL_CAMBIOS {
        uuid id PK
        string entidad
        uuid entidad_id
        uuid usuario_id FK
        string campo_modificado
    }
```
