create table if not exists usuarios (
  id bigint generated always as identity primary key,
  nombre varchar(120) not null,
  email varchar(180) unique not null,
  password_hash varchar(255) not null,
  activo boolean not null default true,
  queseria_id bigint,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists roles (
  id bigint generated always as identity primary key,
  nombre varchar(40) unique not null,
  descripcion varchar(180),
  created_at timestamptz not null default now()
);

create table if not exists usuario_roles (
  usuario_id bigint not null references usuarios(id) on delete cascade,
  rol_id bigint not null references roles(id),
  primary key (usuario_id, rol_id)
);

create index if not exists ix_usuarios_email on usuarios(email);
create index if not exists ix_usuarios_queseria on usuarios(queseria_id);
create index if not exists ix_usuario_roles_rol on usuario_roles(rol_id);

insert into roles (nombre, descripcion)
values
  ('ADMIN', 'Administrador del sistema'),
  ('OPERADOR', 'Usuario operativo'),
  ('LECTOR', 'Usuario de solo lectura')
on conflict (nombre) do nothing;
