-- Proveedores: tipo de identificación (CC, NIT, CE, etc.)
ALTER TABLE proveedores
  ADD COLUMN IF NOT EXISTS tipo_identificacion varchar(10);

-- Default temporal para filas existentes (puedes cambiar luego en UI)
UPDATE proveedores
   SET tipo_identificacion = COALESCE(tipo_identificacion, 'CC');

-- Opcional: si quieres forzar no nulo (recomendado)
ALTER TABLE proveedores
  ALTER COLUMN tipo_identificacion SET NOT NULL;

-- Sugerido (no obligatorio): índice para búsquedas frecuentes
CREATE INDEX IF NOT EXISTS ix_proveedores_tipo_ident ON proveedores(tipo_identificacion);

-- Muestras: agua %, equipo, condición, SNG opcional
ALTER TABLE muestras_leche
  ADD COLUMN IF NOT EXISTS agua_pct numeric(5,2),
  ADD COLUMN IF NOT EXISTS equipo varchar(20),
  ADD COLUMN IF NOT EXISTS condicion varchar(10),
  ADD COLUMN IF NOT EXISTS sng numeric(5,2);

-- Defaults suaves para MVP (puedes dejarlos NULL y validarlo en capa de aplicación)
UPDATE muestras_leche SET equipo = COALESCE(equipo, 'MANUAL');
UPDATE muestras_leche SET condicion = COALESCE(condicion, 'TIBIA');
