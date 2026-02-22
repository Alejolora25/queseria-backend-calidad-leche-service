create table if not exists proveedores (
  id bigint generated always as identity primary key,
  nombre varchar(120) not null,
  identificacion varchar(30) unique not null,
  activo boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists muestras_leche (
  id bigint generated always as identity primary key,
  proveedor_id bigint not null references proveedores(id),
  fecha_muestra timestamptz not null,
  volumen_litros numeric(10,2),
  grasa numeric(5,2),
  proteina numeric(5,2),
  lactosa numeric(5,2),
  solidos_totales numeric(5,2),
  densidad numeric(6,4),
  acidez_dornic numeric(5,2),
  ufc_bacterias integer,
  cc_somaticas integer,
  temperatura_c numeric(5,2),
  precio_litro numeric(10,2),
  observaciones text
);

create index if not exists ix_muestras_prov_fecha on muestras_leche (proveedor_id, fecha_muestra);