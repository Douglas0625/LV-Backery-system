-- ============================================
-- BASE DE DATOS: LV BAKERY
-- Sistema de gestión de panadería
-- PostgreSQL
-- ============================================

-- =========================
-- LIMPIEZA DE TABLAS
-- =========================

DROP TABLE IF EXISTS detalle_venta CASCADE;
DROP TABLE IF EXISTS venta CASCADE;
DROP TABLE IF EXISTS detalle_pedido CASCADE;
DROP TABLE IF EXISTS pedido CASCADE;
DROP TABLE IF EXISTS estado_pedido CASCADE;
DROP TABLE IF EXISTS cliente CASCADE;

DROP TABLE IF EXISTS detalle_receta CASCADE;
DROP TABLE IF EXISTS receta CASCADE;
DROP TABLE IF EXISTS producto CASCADE;

DROP TABLE IF EXISTS movimiento_inventario CASCADE;
DROP TABLE IF EXISTS tipo_movimiento CASCADE;
DROP TABLE IF EXISTS detalle_compra CASCADE;
DROP TABLE IF EXISTS compra CASCADE;
DROP TABLE IF EXISTS ingrediente CASCADE;
DROP TABLE IF EXISTS proveedor CASCADE;

DROP TABLE IF EXISTS gasto CASCADE;
DROP TABLE IF EXISTS categoria_gasto CASCADE;

DROP TABLE IF EXISTS usuario CASCADE;
DROP TABLE IF EXISTS rol CASCADE;


-- =========================
-- TABLA ROL
-- =========================

CREATE TABLE rol (
    id_rol SERIAL PRIMARY KEY,
    nombre_rol VARCHAR(50) NOT NULL UNIQUE
);


-- =========================
-- TABLA USUARIO
-- =========================

CREATE TABLE usuario (
    id_usuario SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    usuario VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    id_rol INT NOT NULL,

    CONSTRAINT fk_usuario_rol
        FOREIGN KEY (id_rol)
        REFERENCES rol(id_rol)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);


-- =========================
-- TABLA CLIENTE
-- =========================

CREATE TABLE cliente (
    id_cliente SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    correo VARCHAR(100)
);


-- =========================
-- TABLA ESTADO_PEDIDO
-- =========================

CREATE TABLE estado_pedido (
    id_estado_pedido SERIAL PRIMARY KEY,
    nombre_estado VARCHAR(50) NOT NULL UNIQUE
);


-- =========================
-- TABLA PRODUCTO
-- =========================

CREATE TABLE producto (
    id_producto SERIAL PRIMARY KEY,
    nombre_producto VARCHAR(100) NOT NULL,
    descripcion TEXT,
    precio_venta NUMERIC(10,2) NOT NULL,
    costo_estimado_unitario NUMERIC(10,2) DEFAULT 0,
    unidades_por_presentacion INT NOT NULL DEFAULT 1,

    CONSTRAINT chk_producto_precio
        CHECK (precio_venta >= 0),

    CONSTRAINT chk_producto_costo
        CHECK (costo_estimado_unitario >= 0),

    CONSTRAINT chk_producto_unidades
        CHECK (unidades_por_presentacion > 0)
);


-- =========================
-- TABLA PEDIDO
-- =========================

CREATE TABLE pedido (
    id_pedido SERIAL PRIMARY KEY,
    id_cliente INT NOT NULL,
    fecha_pedido DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_entrega DATE NOT NULL,
    id_estado_pedido INT NOT NULL,
    descripcion_pedido TEXT,
    total_pedido NUMERIC(10,2) NOT NULL DEFAULT 0,

    CONSTRAINT fk_pedido_cliente
        FOREIGN KEY (id_cliente)
        REFERENCES cliente(id_cliente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_pedido_estado
        FOREIGN KEY (id_estado_pedido)
        REFERENCES estado_pedido(id_estado_pedido)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_pedido_total
        CHECK (total_pedido >= 0)
);


-- =========================
-- TABLA DETALLE_PEDIDO
-- =========================

CREATE TABLE detalle_pedido (
    id_detalle_pedido SERIAL PRIMARY KEY,
    id_pedido INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario NUMERIC(10,2) NOT NULL,
    subtotal NUMERIC(10,2) NOT NULL,

    CONSTRAINT fk_detalle_pedido_pedido
        FOREIGN KEY (id_pedido)
        REFERENCES pedido(id_pedido)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_detalle_pedido_producto
        FOREIGN KEY (id_producto)
        REFERENCES producto(id_producto)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_detalle_pedido_cantidad
        CHECK (cantidad > 0),

    CONSTRAINT chk_detalle_pedido_precio
        CHECK (precio_unitario >= 0),

    CONSTRAINT chk_detalle_pedido_subtotal
        CHECK (subtotal >= 0)
);


-- =========================
-- TABLA VENTA
-- =========================

CREATE TABLE venta (
    id_venta SERIAL PRIMARY KEY,
    id_pedido INT,
    id_cliente INT,
    fecha_venta DATE NOT NULL DEFAULT CURRENT_DATE,
    total_venta NUMERIC(10,2) NOT NULL DEFAULT 0,
    tipo_venta VARCHAR(30) NOT NULL,
    metodo_pago VARCHAR(50) NOT NULL,
    numero_comprobante VARCHAR(50),

    CONSTRAINT fk_venta_pedido
        FOREIGN KEY (id_pedido)
        REFERENCES pedido(id_pedido)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT fk_venta_cliente
        FOREIGN KEY (id_cliente)
        REFERENCES cliente(id_cliente)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT chk_venta_total
        CHECK (total_venta >= 0),

    CONSTRAINT chk_tipo_venta
        CHECK (tipo_venta IN ('DIRECTA', 'PEDIDO'))
);


-- =========================
-- TABLA DETALLE_VENTA
-- =========================

CREATE TABLE detalle_venta (
    id_detalle_venta SERIAL PRIMARY KEY,
    id_venta INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario NUMERIC(10,2) NOT NULL,
    subtotal NUMERIC(10,2) NOT NULL,

    CONSTRAINT fk_detalle_venta_venta
        FOREIGN KEY (id_venta)
        REFERENCES venta(id_venta)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_detalle_venta_producto
        FOREIGN KEY (id_producto)
        REFERENCES producto(id_producto)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_detalle_venta_cantidad
        CHECK (cantidad > 0),

    CONSTRAINT chk_detalle_venta_precio
        CHECK (precio_unitario >= 0),

    CONSTRAINT chk_detalle_venta_subtotal
        CHECK (subtotal >= 0)
);


-- =========================
-- TABLA INGREDIENTE
-- =========================

CREATE TABLE ingrediente (
    id_ingrediente SERIAL PRIMARY KEY,
    nombre_ingrediente VARCHAR(100) NOT NULL UNIQUE,
    stock_actual_gramos NUMERIC(10,2) NOT NULL DEFAULT 0,
    costo_por_gramo NUMERIC(10,4) NOT NULL DEFAULT 0,

    CONSTRAINT chk_ingrediente_stock
        CHECK (stock_actual_gramos >= 0),

    CONSTRAINT chk_ingrediente_costo
        CHECK (costo_por_gramo >= 0)
);


-- =========================
-- TABLA RECETA
-- =========================

CREATE TABLE receta (
    id_receta SERIAL PRIMARY KEY,
    id_producto INT NOT NULL UNIQUE,
    nombre_receta VARCHAR(100) NOT NULL,
    rendimiento_total INT NOT NULL,

    CONSTRAINT fk_receta_producto
        FOREIGN KEY (id_producto)
        REFERENCES producto(id_producto)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT chk_receta_rendimiento
        CHECK (rendimiento_total > 0)
);


-- =========================
-- TABLA DETALLE_RECETA
-- =========================

CREATE TABLE detalle_receta (
    id_detalle_receta SERIAL PRIMARY KEY,
    id_receta INT NOT NULL,
    id_ingrediente INT NOT NULL,
    cantidad_gramos NUMERIC(10,2) NOT NULL,

    CONSTRAINT fk_detalle_receta_receta
        FOREIGN KEY (id_receta)
        REFERENCES receta(id_receta)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_detalle_receta_ingrediente
        FOREIGN KEY (id_ingrediente)
        REFERENCES ingrediente(id_ingrediente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_detalle_receta_cantidad
        CHECK (cantidad_gramos > 0),

    CONSTRAINT uq_receta_ingrediente
        UNIQUE (id_receta, id_ingrediente)
);


-- =========================
-- TABLA PROVEEDOR
-- =========================

CREATE TABLE proveedor (
    id_proveedor SERIAL PRIMARY KEY,
    nombre_proveedor VARCHAR(100) NOT NULL,
    telefono VARCHAR(20)
);


-- =========================
-- TABLA COMPRA
-- =========================

CREATE TABLE compra (
    id_compra SERIAL PRIMARY KEY,
    fecha_compra DATE NOT NULL DEFAULT CURRENT_DATE,
    id_proveedor INT,
    total_compra NUMERIC(10,2) NOT NULL DEFAULT 0,

    CONSTRAINT fk_compra_proveedor
        FOREIGN KEY (id_proveedor)
        REFERENCES proveedor(id_proveedor)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT chk_compra_total
        CHECK (total_compra >= 0)
);


-- =========================
-- TABLA DETALLE_COMPRA
-- =========================

CREATE TABLE detalle_compra (
    id_detalle_compra SERIAL PRIMARY KEY,
    id_compra INT NOT NULL,
    id_ingrediente INT NOT NULL,
    cantidad_gramos NUMERIC(10,2) NOT NULL,
    costo_unitario_gramo NUMERIC(10,4) NOT NULL,
    subtotal NUMERIC(10,2) NOT NULL,

    CONSTRAINT fk_detalle_compra_compra
        FOREIGN KEY (id_compra)
        REFERENCES compra(id_compra)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_detalle_compra_ingrediente
        FOREIGN KEY (id_ingrediente)
        REFERENCES ingrediente(id_ingrediente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_detalle_compra_cantidad
        CHECK (cantidad_gramos > 0),

    CONSTRAINT chk_detalle_compra_costo
        CHECK (costo_unitario_gramo >= 0),

    CONSTRAINT chk_detalle_compra_subtotal
        CHECK (subtotal >= 0)
);


-- =========================
-- TABLA TIPO_MOVIMIENTO
-- =========================

CREATE TABLE tipo_movimiento (
    id_tipo_movimiento SERIAL PRIMARY KEY,
    nombre_tipo VARCHAR(50) NOT NULL UNIQUE
);


-- =========================
-- TABLA MOVIMIENTO_INVENTARIO
-- =========================

CREATE TABLE movimiento_inventario (
    id_movimiento SERIAL PRIMARY KEY,
    id_ingrediente INT NOT NULL,
    id_tipo_movimiento INT NOT NULL,
    fecha_movimiento DATE NOT NULL DEFAULT CURRENT_DATE,
    cantidad_gramos NUMERIC(10,2) NOT NULL,
    descripcion TEXT,
    referencia VARCHAR(100),
    observacion TEXT,

    CONSTRAINT fk_movimiento_ingrediente
        FOREIGN KEY (id_ingrediente)
        REFERENCES ingrediente(id_ingrediente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_movimiento_tipo
        FOREIGN KEY (id_tipo_movimiento)
        REFERENCES tipo_movimiento(id_tipo_movimiento)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_movimiento_cantidad
        CHECK (cantidad_gramos > 0)
);


-- =========================
-- TABLA CATEGORIA_GASTO
-- =========================

CREATE TABLE categoria_gasto (
    id_categoria_gasto SERIAL PRIMARY KEY,
    nombre_categoria VARCHAR(50) NOT NULL UNIQUE
);


-- =========================
-- TABLA GASTO
-- =========================

CREATE TABLE gasto (
    id_gasto SERIAL PRIMARY KEY,
    fecha_gasto DATE NOT NULL DEFAULT CURRENT_DATE,
    id_categoria_gasto INT NOT NULL,
    descripcion TEXT,
    monto NUMERIC(10,2) NOT NULL,

    CONSTRAINT fk_gasto_categoria
        FOREIGN KEY (id_categoria_gasto)
        REFERENCES categoria_gasto(id_categoria_gasto)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_gasto_monto
        CHECK (monto > 0)
);


-- =========================
-- DATOS INICIALES
-- =========================

INSERT INTO rol (nombre_rol) VALUES
('Administrador'),
('Cajero');


INSERT INTO estado_pedido (nombre_estado) VALUES
('Pendiente'),
('En producción'),
('Listo'),
('Entregado'),
('Cancelado');


INSERT INTO tipo_movimiento (nombre_tipo) VALUES
('Compra'),
('Producción'),
('Ajuste entrada'),
('Ajuste salida'),
('Merma');


INSERT INTO categoria_gasto (nombre_categoria) VALUES
('Servicios'),
('Empaques'),
('Transporte'),
('Mantenimiento'),
('Otros');


-- Usuario inicial de prueba
-- Luego pueden cambiar la contraseña desde el sistema.
INSERT INTO usuario (nombre, usuario, password, id_rol) VALUES
('Administrador General', 'admin', 'admin123', 1),
('Cajero General', 'cajero', 'cajero123', 2);


-- =========================
-- DATOS DE PRUEBA OPCIONALES
-- =========================

INSERT INTO cliente (nombre, telefono, correo) VALUES
('Cliente General', '0000-0000', 'cliente@correo.com');


INSERT INTO proveedor (nombre_proveedor, telefono) VALUES
('Proveedor General', '0000-0000');


INSERT INTO ingrediente (nombre_ingrediente, stock_actual_gramos, costo_por_gramo) VALUES
('Harina', 5000, 0.0025),
('Azúcar', 3000, 0.0030),
('Mantequilla', 2000, 0.0100),
('Huevos', 1000, 0.0050),
('Chocolate', 1500, 0.0120);


INSERT INTO producto (
    nombre_producto,
    descripcion,
    precio_venta,
    costo_estimado_unitario,
    unidades_por_presentacion
) VALUES
('Alfajores', 'Caja de alfajores artesanales', 6.00, 2.50, 6),
('Brownies', 'Porción de brownies de chocolate', 8.00, 3.25, 8);


-- Recetas de prueba
INSERT INTO receta (id_producto, nombre_receta, rendimiento_total) VALUES
(1, 'Receta Alfajores', 6),
(2, 'Receta Brownies', 8);


INSERT INTO detalle_receta (id_receta, id_ingrediente, cantidad_gramos) VALUES
(1, 1, 500),
(1, 2, 200),
(1, 3, 250),
(2, 1, 300),
(2, 2, 250),
(2, 3, 200),
(2, 5, 300);


-- =========================
-- VERIFICACIÓN
-- =========================

SELECT 'Base de datos LV Bakery creada correctamente' AS mensaje;