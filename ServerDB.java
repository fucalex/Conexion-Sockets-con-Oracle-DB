import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
/**
 *
 * @author Subgrupo 14 Persistencia Transaccional POLI - 2022
 */
public class ServerDB {
    
// Socket para el servidor.
	private static ServerSocket serverSocket = null;
	// Socket para el cliente.
	private static Socket clientSocket = null;
	public static ArrayList<clientThread> clients = new ArrayList<clientThread>();
	public static void main(String args[]) {
            
                 System.out.println("////////////////////////////////////////////////////////////////////////////////////\n"
                                     + "////SERVIDOR (SOCKETS) - © 2022 GRUPO 14 Persistencia y Datos Transaccionales///\n"
                                     + "////////////////////////////////////////////////////////////////////////////////////\n");

		// Puerto por defecto y/o asignado por el usuario
		int portNumber = 1234;


		if (args.length < 1) 
		{

			System.out.println("No se especificó el puerto.\nEl servidor usará el puerto asignado por defecto: " + portNumber);

		} 
		else 
		{
			portNumber = Integer.valueOf(args[0]).intValue();

			System.out.println("El servidor usará el puerto asignado por el usuario: " + portNumber);
		}

                System.out.println("Esperando conexión entrante en el puerto " + portNumber + "...");

		/*
		 * Abre un socket de servidor en el puerto asignado
		 */
		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println("No se puede crear el socket de servidor");
		}
		/*
		 * Por cada conexion, se crea un socket de cliente y se maneja como un nuevo hilo
                                    * Hilo: Linea por la cual se establece la conexion
		 */
		int clientNum = 1;
		while (true) {
			try {
				clientSocket = serverSocket.accept();
				clientThread curr_client =  new clientThread(clientSocket, clients);
				clients.add(curr_client);
				curr_client.start();
				System.out.println("El cliente "  + clientNum + " se ha conectado!");
				clientNum++;

			} catch (IOException e) {

				System.out.println("No se puede establecer conexion con el cliente");
			}
		}
	}
}
/*
 * Esta clase, gestiona clientes individualmente en sus respectivos hilos 
 * abriedo flujos separados de entrada y salida. 
 */
class clientThread extends Thread {

	private String clientName = null;
	private ObjectInputStream is = null;
	private ObjectOutputStream os = null;
	private Socket clientSocket = null;
	private final ArrayList<clientThread> clients;
	public clientThread(Socket clientSocket, ArrayList<clientThread> clients) {

		this.clientSocket = clientSocket;
		this.clients = clients;
	}
	public void run() {

		ArrayList<clientThread> clients = this.clients;
		try {
			/*
			 * Creacion de los flujos de entrada y salida de informacion del cliente.
			 */
			is = new ObjectInputStream(clientSocket.getInputStream());
			os = new ObjectOutputStream(clientSocket.getOutputStream());

			String name;
                                                     int u;
			while (true) {
				synchronized(this)
				{
                    //El servidor pide el nombre de usuario al cliente una vez que este inicia el proceso de conexion
					this.os.writeObject("Por favor, ingrese su nombre :");
					this.os.flush();
					name = ((String) this.is.readObject()).trim();
                                        
                                                                                        

if ((name.indexOf('@') == -1) || (name.indexOf('!') == -1)) {
						break;
					} else {
						this.os.writeObject("el nombre no debe tener los siguientes caracteres: '@' o '!' ");
						this.os.flush();
					}
				}
			}
                /* Bienvenida al nuevo cliente */
				System.out.println("El nombre del cliente es: " + name); 

				this.os.writeObject("*** Bienvenido/a " + name + " al chat del grupo 14 ***\nEscriba #chao para salir del chat");
				this.os.flush();

				this.os.writeObject("Registro de cliente creado");
				this.os.flush();
				synchronized(this)
				{
				for (clientThread curr_client : clients)  
				{
					if (curr_client != null && curr_client == this) {
						clientName = "@" + name;
						break;
					}
				}
				for (clientThread curr_client : clients) {
					if (curr_client != null && curr_client != this) {
						curr_client.os.writeObject(name + " se ha unido");
						curr_client.os.flush();
					}
				}
			}
			/* Inicio de la conversacion. */
			while (true) {

				this.os.writeObject("Escriba su mensaje:");
				this.os.flush();

				String line = (String) is.readObject();
                                
            //Lista de clientes conectados para ser consultada por el cliente 
            ArrayList<String> l = new ArrayList<>();
            int qty = clients.size();
            for(u=0;u < qty; u++){
            //String a =name.nextLine();
            l.add(name);
            }

                                                                       
            // enviar listado de clientes conectados
            if (line.startsWith("#clientes")) {
                                                                                System.out.println(l);
                                                                                //this.os.writeObject(l);
                                                                                //this.os.flush();
                                                                                //listaclientes(line,name);
                                                                                //System.out.println(Arrays.toString(clients.toArray()));
				}
				if (line.startsWith("#chao")) {
					break;
				}
                                
                                                                      //Si el mensaje va para la base de datos, se envia poniendo “>” precedido de “:” y la sentencia tal y como se ingresaría en SQL. Esto a su vez, active la function “database” expicada mas abajo
                    
if (line.startsWith(">")) {
					database(line,name); 
				}
                                
                                
                                
				/* Si el mensaje es privado, se envia al cliente especificado usando @ + nombre del cliente + : + mensaje */
				if (line.startsWith("@")) {
					unicast(line,name);        	
				}
				else // si es a todos, se envia nadamas escribiendo el mensaje
				{
					broadcast(line,name);
				}
			}
                        
			/* Termina la sesion para el usuario especificado */
			this.os.writeObject("*** Abrase " + name + " ***");
			this.os.flush();
			System.out.println(name + " desconectado.");
			clients.remove(this);

			synchronized(this) {
				if (!clients.isEmpty()) {
					for (clientThread curr_client : clients) {
						if (curr_client != null && curr_client != this && curr_client.clientName != null) {
							curr_client.os.writeObject("*** El usuario " + name + " se ha desconectado ***");
							curr_client.os.flush();
						}
					}
				}
			}
			this.is.close();
			this.os.close();
			clientSocket.close();

		} catch (IOException e) {
			System.out.println("Se ha terminado la sesion del usuario");

		} catch (ClassNotFoundException e) {
			System.out.println("Class Not Found");
		}
	}

        /**** Esta funcion transmite mensajes a todos los usuarios conectados al servidor ***/
	void broadcast(String line, String name) throws IOException, ClassNotFoundException {

			/* Enviar mensaje a todos los usuarios (Broadcast)*/
			synchronized(this){
				for (clientThread curr_client : clients) {
					if (curr_client != null && curr_client.clientName != null && curr_client.clientName!=this.clientName) 
					{
						curr_client.os.writeObject("<" + name + "> " + line);
						curr_client.os.flush();
					}
				}
   //registro de envío a todos los usuarios en la consola. Se captura el nombre de quien envió el mensaje a todos
				this.os.writeObject("Mensaje enviado.");
				this.os.flush();
				System.out.println("Mensaje enviado por " + this.clientName.substring(1));
			}
	}
	/**** Esta funcion transmite mensajes a un usuario en especifico, establecido por el cliente (unicast) ***/	

	void unicast(String line, String name) throws IOException, ClassNotFoundException {

		String[] words = line.split(":", 2); 

		/* Enviando mensaje a un unico cliente*/
			if (words.length > 1 && words[1] != null) {
				words[1] = words[1].trim();
				if (!words[1].isEmpty()) {
					for (clientThread curr_client : clients) {
						if (curr_client != null && curr_client != this && curr_client.clientName != null
								&& curr_client.clientName.equals(words[0])) {
							curr_client.os.writeObject("<" + name + "> " + words[1]);
							curr_client.os.flush();

							System.out.println(this.clientName.substring(1) + " Enviando mensaje a "+ curr_client.clientName.substring(1));

							/* Avisar al cliente que el mensaje se enció al cliente que eligió*/
							this.os.writeObject("Mensaje enviado a: " + curr_client.clientName.substring(1));
							this.os.flush();
							break;
						}
					}
				}
			}
		}
  
/**** Esta funcion captura lo que hay que enviar a la base de datos.
        sentencia[1] contiene lo que se escriba despues de ":", por lo tanto, se puede escribir la sentencia y hacer que el conector de la BD tome dicha sentencia y la envie a la BD
***/	

        void database(String line, String name) throws IOException, ClassNotFoundException {

		String[] sentencia = line.split(":", 2); 

                    for (int i = 1;; i++) {
                        //RECONOCE QUE EL MENSAJE VA A LA DB
			if (sentencia[0] != null && sentencia.length > 1 && sentencia[1] != null) {
				sentencia[1] = sentencia[1].trim();
                                
                                //parametros de conexion a la DB
                            try (Connection con = DriverManager.getConnection(
                                    "jdbc:oracle:thin:@localhost:1521:XE","USERNAME","PASSWORD")) {

                                    if (con !=null){
                                        System.out.println("***Conectado a la base de datos***");
                                        //creacion de declaracion para conexion y solicitud a DB
                                        Statement st = con.createStatement();
                                        //ejecutar sentencia para obtener info de DB
                                        ResultSet rs=st.executeQuery(sentencia[1].trim());
                                        while(rs.next()) {
                                            
                                        this.os.writeObject(rs.getString(1)+"\t"+rs.getString(2)+"\t"+rs.getString(3)+"\t"+rs.getString(4)+"\t"
                                        +rs.getString(5)+"\t"+rs.getString(6)+"\t"+rs.getString(7)+"\t"+rs.getString(8)+"\t"+rs.getString(9));
                                        this.os.flush();    
                                            

                                               }
                                        //cerrar conexion
                                        rs.close();
                                        st.close();
                                        con.close();

                                        }else{
                                            System.out.println("conexion fallida");
                                        }
                                            }catch (SQLException e) {
                                                System.err.format("SQL State: %s\n%s", e.getSQLState(),e.getMessage());
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                
                                //INFORMA AL USUARIO QUE SU MENSAJE FUE ENVIADO
				this.os.writeObject("Mensaje enviado a Base de datos: " + sentencia[1]);
				this.os.flush();
                                break;
                        }
	}
    }

}
