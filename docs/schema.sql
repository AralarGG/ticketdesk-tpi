-- TicketDesk: esquema de la base de datos (PostgreSQL 14 o superior)
--
-- Este script crea la estructura completa. No es obligatorio ejecutarlo:
-- el backend crea y actualiza las tablas solo al iniciar
-- (spring.jpa.hibernate.ddl-auto=update). Sirve como referencia del modelo
-- documentado en DER-ticketdesk.md y para crear la base a mano.
--
-- Uso:
--   1. Crear la base:   CREATE DATABASE ticketdesk;
--   2. Conectarse a ella y ejecutar este archivo completo.
--
-- Los datos de prueba (empresa demo, categorías y 4 usuarios) los carga el
-- backend en el primer arranque mediante DataInitializer, porque las
-- contraseñas se guardan con hash BCrypt.

CREATE TABLE empresas (
    id          UUID PRIMARY KEY,
    nombre      VARCHAR(150) NOT NULL,
    rubro       VARCHAR(100),
    fecha_alta  TIMESTAMP(6),
    activa      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE configuracion_empresa (
    id                            UUID PRIMARY KEY,
    empresa_id                    UUID NOT NULL UNIQUE REFERENCES empresas (id),
    modulos_habilitados           JSONB,
    ventana_mantenimiento_inicio  TIME(6),
    ventana_mantenimiento_fin     TIME(6)
);

CREATE TABLE usuarios (
    id             UUID PRIMARY KEY,
    empresa_id     UUID NOT NULL REFERENCES empresas (id),
    nombre         VARCHAR(150) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    rol            VARCHAR(255) NOT NULL
                   CHECK (rol IN ('ROLE_USER', 'ROLE_AGENT', 'ROLE_SUPERVISOR', 'ROLE_ADMIN')),
    activo         BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_alta     TIMESTAMP(6),
    CONSTRAINT uk_usuarios_empresa_email UNIQUE (empresa_id, email)
);

CREATE TABLE usuario_roles (
    id                UUID PRIMARY KEY,
    usuario_id        UUID NOT NULL REFERENCES usuarios (id),
    rol               VARCHAR(255) NOT NULL
                      CHECK (rol IN ('ROLE_USER', 'ROLE_AGENT', 'ROLE_SUPERVISOR', 'ROLE_ADMIN')),
    fecha_asignacion  TIMESTAMP(6),
    CONSTRAINT uk_usuario_roles_usuario_rol UNIQUE (usuario_id, rol)
);

-- empresa_id nulo significa categoría genérica, compartida por todas las empresas.
CREATE TABLE categorias (
    id           UUID PRIMARY KEY,
    empresa_id   UUID REFERENCES empresas (id),
    nombre       VARCHAR(100) NOT NULL,
    descripcion  TEXT,
    CONSTRAINT uk_categorias_empresa_nombre UNIQUE (empresa_id, nombre)
);

CREATE TABLE tickets (
    id                      UUID PRIMARY KEY,
    empresa_id              UUID NOT NULL REFERENCES empresas (id),
    usuario_id              UUID NOT NULL REFERENCES usuarios (id),
    agente_id               UUID REFERENCES usuarios (id),
    categoria_id            UUID NOT NULL REFERENCES categorias (id),
    titulo                  VARCHAR(200) NOT NULL,
    descripcion             TEXT,
    canal_origen            VARCHAR(255) NOT NULL
                            CHECK (canal_origen IN ('FORMULARIO', 'VOZ')),
    audio_url               VARCHAR(500),
    transcripcion_original  TEXT,
    prioridad               VARCHAR(255) NOT NULL
                            CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA')),
    nivel_atencion          VARCHAR(255) NOT NULL
                            CHECK (nivel_atencion IN ('NIVEL_1', 'NIVEL_2', 'NIVEL_3', 'CRITICO')),
    estado                  VARCHAR(255) NOT NULL
                            CHECK (estado IN ('NUEVO', 'ASIGNADO', 'EN_PROGRESO', 'ESPERANDO_CLIENTE',
                                              'ESCALADO', 'RESUELTO', 'REABIERTO', 'CERRADO')),
    fecha_resuelto          TIMESTAMP(6),
    reincidente             BOOLEAN NOT NULL DEFAULT FALSE,
    es_reclamo_formal       BOOLEAN NOT NULL DEFAULT FALSE,
    responsable_externo     VARCHAR(200),
    fecha_creacion          TIMESTAMP(6),
    fecha_actualizacion     TIMESTAMP(6)
);

CREATE TABLE comentarios (
    id          UUID PRIMARY KEY,
    ticket_id   UUID NOT NULL REFERENCES tickets (id),
    usuario_id  UUID NOT NULL REFERENCES usuarios (id),
    contenido   TEXT NOT NULL,
    fecha       TIMESTAMP(6)
);

CREATE TABLE adjuntos (
    id             UUID PRIMARY KEY,
    ticket_id      UUID NOT NULL REFERENCES tickets (id),
    comentario_id  UUID REFERENCES comentarios (id),
    url            VARCHAR(500) NOT NULL,
    tipo           VARCHAR(50),
    fecha_subida   TIMESTAMP(6)
);

-- Tabla genérica de auditoría. entidad_id apunta a distintas tablas según el
-- valor de entidad, por eso no tiene clave foránea (ver DER-ticketdesk.md).
-- usuario_id nulo indica un cambio automático del sistema.
CREATE TABLE historial_cambios (
    id                UUID PRIMARY KEY,
    entidad           VARCHAR(100) NOT NULL,
    entidad_id        UUID NOT NULL,
    campo_modificado  VARCHAR(100) NOT NULL,
    valor_anterior    VARCHAR(255),
    valor_nuevo       VARCHAR(255),
    usuario_id        UUID REFERENCES usuarios (id),
    motivo            TEXT,
    fecha             TIMESTAMP(6)
);

-- Índices para las consultas más frecuentes (listados filtrados y detalle).
CREATE INDEX idx_tickets_empresa_estado ON tickets (empresa_id, estado);
CREATE INDEX idx_tickets_usuario ON tickets (usuario_id);
CREATE INDEX idx_tickets_agente ON tickets (agente_id);
CREATE INDEX idx_comentarios_ticket ON comentarios (ticket_id);
CREATE INDEX idx_adjuntos_ticket ON adjuntos (ticket_id);
CREATE INDEX idx_historial_entidad ON historial_cambios (entidad, entidad_id);
