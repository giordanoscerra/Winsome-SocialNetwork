import java.io.*;
import java.net.*;

//classe thread per i pagamenti in multicast
public class ServerMulticastThread implements Runnable {
	//mantiene un riferimento al server winsome
	WinsomeServer winsome;
	//costruttore
	ServerMulticastThread(WinsomeServer winsome) {
		this.winsome = winsome;
	}
	
	@Override
	public void run() {
		//preparo il messaggio da mandare in multicast a tutti i client in ascolto
		byte[] messaggio = "il tuo portafoglio è stato aggiornato !".getBytes(); 
		
		//mi preparo a connettermi con le credenziali multicast lette dal file configurazione server
		InetAddress indirizzo_multicast = null;
		try {
			indirizzo_multicast = InetAddress.getByName(winsome.configurazione.getIndirizzoMulticast());
		} catch (UnknownHostException e1) {
			//errore
			System.out.println("host multicast non valido");
			System.exit(-1);
		}
		
		//preparo il pacchetto da inviare
		DatagramPacket pacchetto = new DatagramPacket(messaggio, messaggio.length, indirizzo_multicast, winsome.configurazione.getPortaMulticast());
		//try with resources, apro il socket
		try (DatagramSocket socket = new DatagramSocket(winsome.configurazione.getPortaUdp());) {
		
			//ciclo infinito
			while (true) {
				try {
					//dormo per tot secodi, letti dal file di configurazione
					Thread.sleep(winsome.configurazione.getTimerPremiazioni());
				} catch (InterruptedException e) {
						//esco, sono stato interrotto
						System.out.println("thread premiazioni interrotto");
						break;
				}
				
				//chiamo il metodo di winsome per i pagamenti: scorrera' tutti i post
				winsome.aggiorna_portafogli();
				
				System.out.println("\tportafogli aggiornati !");
				
				//invio il messaggio a tutti i client in ascolto
				try{
					socket.send(pacchetto);
				}
				catch(IOException ex) {
					//errore, esco
					System.out.println(ex);
					break;
				}
			}
		
		} catch (SocketException e1) {
			//errore fatale
			System.out.println("errore fatale multicast");
			System.exit(-1);
		}
	}
}