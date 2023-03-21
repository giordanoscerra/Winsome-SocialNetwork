import java.io.*;
import java.net.*;

//classe thread per riceve messaggi multicast dal server winsome
public class ClientMulticastThread implements Runnable {

	// socket multicast
	MulticastSocket socket = null;
	// gruppo multicast
	InetSocketAddress gruppo = null;
	// interfaccia di rete utilizzata per il multicast
	NetworkInterface net_interface = null;

	// costruttore
	public ClientMulticastThread(MulticastSocket socket, InetSocketAddress gruppo, NetworkInterface net_interface) {
		this.socket = socket;
		this.gruppo = gruppo;
		this.net_interface = net_interface;
	}

	@Override
	public void run() {
		// inizio thread multicast

		// array di byte per ricevere il messaggio dal server
		byte[] buffer_messaggio = new byte[64];
		// ciclo fino a quando non vengo interrotto lato client
		while (!Thread.interrupted()) {
			try {
				// datagramma udp per ricevere il messaggio
				DatagramPacket pacchetto = new DatagramPacket(buffer_messaggio, buffer_messaggio.length);
				// chiamata BLOCCANTE di lettura sul socket: aspetto che arrivi il messaggio
				socket.receive(pacchetto);
				// recupero il messaggio
				String messaggio = new String(pacchetto.getData());
				// stampo la notifica
				System.out.println("<< " + messaggio.trim());
			} catch (IOException e) {
				//ho chiuso il socket lato client: chiudo il thread
				
			}
		}
	}
}
