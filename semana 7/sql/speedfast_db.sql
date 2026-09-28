-- ---------------------------------------------------------------------------
-- SpeedFast - Semana 7
-- Base de datos de pedidos, repartidores y entregas.
--
-- Ejecutar completo en MySQL Workbench o por consola:
--     mysql -u root -p < speedfast_db.sql
--
-- Las tres tablas y sus columnas son las del modelo de la actividad. Las
-- columnas marcadas como "extra" se agregaron para no perder los datos que la
-- interfaz de la semana 6 ya pedia (distancia, datos propios de cada tipo de
-- pedido). Todas admiten NULL, asi que el modelo original sigue funcionando
-- tal cual aunque no se usen.
--
-- Autora: Olga Rivas
-- ---------------------------------------------------------------------------

CREATE DATABASE IF NOT EXISTS speedfast_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE speedfast_db;

-- Se borran en orden inverso a las llaves foraneas: primero la tabla que
-- apunta a las otras, si no MySQL rechaza el DROP.
DROP TABLE IF EXISTS entrega;
DROP TABLE IF EXISTS pedido;
DROP TABLE IF EXISTS repartidor;

-- ---------------------------------------------------------------------------
-- repartidor
-- ---------------------------------------------------------------------------
CREATE TABLE repartidor (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    nombre   VARCHAR(100) NOT NULL,
    vehiculo VARCHAR(100) NULL              -- extra: el vehiculo que ya mostraba la interfaz
);

-- ---------------------------------------------------------------------------
-- pedido
-- ---------------------------------------------------------------------------
CREATE TABLE pedido (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    direccion    VARCHAR(150) NOT NULL,
    tipo         VARCHAR(30)  NOT NULL,     -- COMIDA | ENCOMIENDA | EXPRESS
    estado       VARCHAR(20)  NOT NULL,     -- PENDIENTE | EN_REPARTO | ENTREGADO (+ CANCELADO, extra)

    -- extra: datos que la interfaz de la semana 6 ya pedia en el formulario
    codigo         VARCHAR(20)   NULL,      -- el P-001 que se ve en la tabla
    distancia_km   DECIMAL(6,2)  NULL,
    restaurante    VARCHAR(100)  NULL,      -- solo pedidos de comida
    peso_kg        DECIMAL(7,2)  NULL,      -- solo encomiendas
    embalaje       VARCHAR(50)   NULL,      -- solo encomiendas
    local_compra   VARCHAR(100)  NULL,      -- solo compras express
    prioritario    TINYINT(1)    NULL,      -- mochila termica / repartidor inmediato
    estado_detalle VARCHAR(20)   NULL,      -- extra: el estado exacto del sistema

    CONSTRAINT uq_pedido_codigo UNIQUE (codigo),
    CONSTRAINT ck_pedido_tipo   CHECK (tipo IN ('COMIDA', 'ENCOMIENDA', 'EXPRESS')),
    -- Los tres primeros son los del modelo de la actividad. CANCELADO se
    -- agrega porque la jerarquia de pedidos permite cancelar desde la semana 3
    -- y la interfaz tiene ese boton.
    CONSTRAINT ck_pedido_estado CHECK (estado IN ('PENDIENTE', 'EN_REPARTO', 'ENTREGADO', 'CANCELADO')),
    CONSTRAINT ck_pedido_distancia CHECK (distancia_km IS NULL OR distancia_km >= 0)
);

-- ---------------------------------------------------------------------------
-- entrega
--
-- Es la tabla que relaciona las otras dos: cada entrega apunta a un pedido y
-- a un repartidor. Un repartidor puede tener muchas entregas, y un pedido
-- puede tener mas de una si se simulan varios intentos.
-- ---------------------------------------------------------------------------
CREATE TABLE entrega (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido     INT  NOT NULL,
    id_repartidor INT  NOT NULL,
    fecha         DATE NOT NULL,
    hora          TIME NOT NULL,

    CONSTRAINT fk_entrega_pedido
        FOREIGN KEY (id_pedido)     REFERENCES pedido(id),
    CONSTRAINT fk_entrega_repartidor
        FOREIGN KEY (id_repartidor) REFERENCES repartidor(id)
);

-- ---------------------------------------------------------------------------
-- Datos iniciales: los repartidores de la empresa.
-- ---------------------------------------------------------------------------
INSERT INTO repartidor (nombre, vehiculo) VALUES
    ('Camila Soto',   'moto con mochila termica'),
    ('Diego Fuentes', 'furgon de carga'),
    ('Luis Vera',     'bicicleta electrica');
