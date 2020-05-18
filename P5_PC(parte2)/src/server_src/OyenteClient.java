package server_src;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import msg_src.Mensaje;
import msg_src.MsgCerrarConexion;
import msg_src.MsgConexion;
import msg_src.MsgConfirmListaUsuarios;
import msg_src.MsgConfirmacionCerrarConexion;
import msg_src.MsgConfirmacionConexion;
import msg_src.MsgEmitirFichero;
import msg_src.MsgErrorConexion;
import msg_src.MsgListaUsuarios;import msg_src.MsgPedirFichero;
import msg_src.MsgPreparadoCS;
import msg_src.MsgPreparadoSC;
import users_src.FlujosUsuario;
import users_src.Usuario;

public class OyenteClient extends Thread {
	
	private Socket socket;
	private ObjectOutputStream f_out;//flujo salida hacia cliente
	private ObjectInputStream f_in; // flujo entrada a servidor
	private Server server;
	
	public OyenteClient(Socket s,Server server) {
		
		try {
			this.socket = s;
			this.f_out= new ObjectOutputStream(socket.getOutputStream());
			this.f_in = new  ObjectInputStream(socket.getInputStream());
			this.server= server;
		} catch (IOException e) {
			System.out.println("PROBLEMA EN LA CREACION DE OYENTE CLIENTE");
			e.printStackTrace();
		}
		
	}
	
	
	public void run() {
		
		Mensaje m = null;
		while(true) {
			try {
				
				m = (Mensaje) f_in.readObject();
				
				switch(m.getMensaje()) {
					case "MENSAJE_CONEXION":{
						realizarConexion( (MsgConexion) m);
						break;
					}
					case "MENSAJE_LISTA_USARIOS":{
						System.out.println("Cliente "+ ((MsgListaUsuarios) m).getIdUsuario()+" ha solicitado info usuarios");
						f_out.writeObject(new MsgConfirmListaUsuarios(m.getIPDestino(), m.getIPOrigen(),server.getUsersInfo()));
						break;
					}
					case "MENSAJE_CERRAR_CONEXION":{
						cerrarConexion((MsgCerrarConexion) m);
						return;
					}
					case "MENSAJE_PEDIR_FICHERO":{
						avisarPeerEmisor((MsgPedirFichero) m);
						break;
					}
					case "MENSAJE_PREPARADO_CLIENTESERVIDOR":{
						avisarPeerReceptor((MsgPreparadoCS) m);
						break;
					}
					
				}
								
			} catch (Exception e) {
				System.out.println("Algo falla en un OyenteClient,cerrando su conexion");
				server.deleteInfoUsuario(m.getIdUsuario());
				server.deleteFlujosUsuario(m.getIdUsuario());
				e.printStackTrace();
				return;
			}
		}
		
		
		
	}
	
	private void realizarConexion(MsgConexion msg) throws IOException {
		//info usuario
		if(server.userAlreadyExists(msg.getIdUsuario())) {
			f_out.writeObject(new MsgErrorConexion(server.getIpServer(),msg.getIPOrigen(),msg.getFicheros()));
		}
		else {
			Usuario u = new Usuario(msg.getIdUsuario(),msg.getIPOrigen(),msg.getFicheros());
			//id y canales usuario
			FlujosUsuario fu= new FlujosUsuario(msg.getIdUsuario(),f_out,f_in);
			server.añadirUsuario(u);
			server.añadirFlujosUsuario(fu);
			//enviamos mensaje confirmacion
			f_out.writeObject(new MsgConfirmacionConexion(msg.getIPDestino(),msg.getIPOrigen()));//del server al cliente
		}
		
	}
	
	private void cerrarConexion(MsgCerrarConexion msg) throws IOException {
		
		System.out.println("Servidor cerrando conexion con " + msg.getIdUsuario());
		f_out.writeObject(new MsgConfirmacionCerrarConexion(msg.getIPDestino(),msg.getIPOrigen(),msg.getIdUsuario()));
		server.deleteInfoUsuario(msg.getIdUsuario());
		server.deleteFlujosUsuario(msg.getIdUsuario());
		f_out.close();
	}
	
	private void avisarPeerEmisor(MsgPedirFichero msg) throws IOException {
		String id_user = server.getOwnerFile(msg.getFilename());
		ObjectOutputStream f_out2 = server.getOutputStreamOC(id_user);
		f_out2.writeObject(new MsgEmitirFichero(msg.getFilename(),msg.getIdUsuario()));
		
	}
	
	private void avisarPeerReceptor(MsgPreparadoCS msg) throws IOException {
		ObjectOutputStream f_out1 = server.getOutputStreamOC(msg.getIdUsuario());
		f_out1.writeObject(new MsgPreparadoSC(msg.getMyIP(),msg.getPuertoPropio(),
				msg.getFilename()));
		
	}

}
