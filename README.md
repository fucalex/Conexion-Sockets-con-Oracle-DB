# Conexion-Sockets-con-Oracle-DB

Esta es una demostracion de como realizar una conexion desde un socket con una base de datos Oracle. La idea es que el servidor resuelva las petiiones contra la base de datos y que el cliente, pueda enviar sentencias/queries desde su terminal hacia el servidor para que de ahi viajen a la base de datos.

Al ser un ejercicio netamente demostrativo, obviamos detalles que pueden ser pulidos para otro tipo de aplicaciones.

Se debe tener en cuenta, establecer los parametros de conexion (usuario y contraseña) de la base de datos, antes de ejecutarla.

Por otro lado, la forma en la que el server reconoce que se va a enviar una sentencia a la BD, es con ">:". Despues del ":", se escribe la sentencia/query
