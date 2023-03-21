import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import com.fasterxml.jackson.databind.ObjectMapper;

//MAIN client
public class MainClient {
	// nome utente
	private static String utente_client = null;
	// flag per l'uscita dal ciclo while
	private static boolean exit = false;
	// interfaccia server rmi per registrarsi
	private static InterfacciaRmiServer server_rmi = null;
	// riferimento al proprio oggetto rmi con lista followers in cache
	private static ServerCallback client_rmi = null;
	// interfaccia server per la callback
	private static InterfacciaServerCallback client_callback = null;
	// thread multicast
	private static Thread multicast_thread = null;
	// socket multicast
	private static MulticastSocket multicast_socket = null;
	// gruppo multicast
	private static InetSocketAddress multicast_gruppo = null;
	// interfaccia di rete multicast
	private static NetworkInterface network_interface = null;
	// oggetto configurazione client
	private static ConfigurazioneClient configurazione = null;
	// indirizzo multicast
	private static String indirizzo_multicast = null;
	// porta multicast
	private static int porta_multicast;
	// object mapper per la conversione in json
	private static ObjectMapper mapper = new ObjectMapper();

	public static void main(String[] args) {
		// leggo il file di configurazione e creo l'oggetto
		configurazione = new ConfigurazioneClient();
		try {
			configurazione = mapper.readValue(Paths.get("json/JSONconfigurazioneclient.json").toFile(),
					ConfigurazioneClient.class);
		} catch (IOException e1) {
			// se c'e' stato un problema esco
			System.out.println("errore fatale nel parsing del file configurazione. esco");
			e1.printStackTrace();
			System.exit(-1);
		}

		// preparo la connessione con gli oggetti RMI
		Remote remote;
		try {
			// recupero il registro
			Registry r = LocateRegistry.getRegistry(configurazione.getRegistryPort());
			// cerco l'oggetto con il suo nome
			remote = r.lookup(configurazione.getOggettoRmi());
			// creo l'oggetto che potro' consultare lato client
			server_rmi = (InterfacciaRmiServer) remote;
			// creo la mia interfaccia callback con caching dei followers
			client_rmi = new ServerCallback();
			// la espongo al server
			client_callback = (InterfacciaServerCallback) UnicastRemoteObject.exportObject(client_rmi, 0);

		} catch (Exception e) {
			// errore rmi, esco
			System.out.println("! errore fatale RMI ");
			System.exit(-1);
		}

		// apro la connessione TCP con il server
		try (SocketChannel socket_client = SocketChannel
				.open(new InetSocketAddress(configurazione.getIndirizzoServer(), configurazione.getTcpPort()));) {

			System.out.println(
					"Benvenuto in winsome, effettua il login per poter accedere al server !\nDigita exit per uscire :)");
			// nuovo scanner da tastiera
			Scanner in = new Scanner(System.in);
			// ciclo while infinito fino a lettura "exit"
			while (!exit) {
				// leggo la richiesta dalla tastiera
				String richiesta = leggi_richiesta(in);
				// se devo mandare qualcosa al server, la mando
				if (richiesta != null) {
					// preparo il buffer per mandare la richiesta con java nio
					ByteBuffer buffer_richiesta = ByteBuffer.wrap(richiesta.getBytes());
					// scrivo il contenuto sul socket
					socket_client.write(buffer_richiesta);
					// pulisco il buffer
					buffer_richiesta.clear();
					// alloco un po' di spazio per la risposta
					ByteBuffer risposta = ByteBuffer.allocate(1024 * 64);
					// leggo la risposta del client
					socket_client.read(risposta);
					// gestisco la risposta ritrasformandola in un oggetto Risposta
					gestoreRispostaServer(mapper.readValue(new String(risposta.array()), Risposta.class));
				}
			}
			// chiudo l'input da tastiera
			in.close();
			System.out.println("bye");
			// chiudo le connessioni RMI
			UnicastRemoteObject.unexportObject(client_rmi, true);

		} catch (IOException e) {
			// esco se il server va giu'
			System.out.println("connessione con il server persa: bye");
			System.exit(-1);
		}
	}

	// legge la richiesta da tastiera
	public static String leggi_richiesta(Scanner in) {
		try {
			// recupera tutta la linea di comando
			String richiesta = in.nextLine();
			// frammenta la richiesta
			StringTokenizer tokenizer = new StringTokenizer(richiesta);
			// recupera l'operazione
			String op = tokenizer.nextToken();
			// switch sull'operazione
			switch (op) {
				case "help":
					System.out.print(

							"register <nome_utente> <password> \n"
									+ "login <nome_utente> <password>\n"
									+ "logout\n"
									+ "list following\n"
									+ "list followers\n"
									+ "list users\n"
									+ "follow <id_utente>\n"
									+ "unfollow <id_utente>\n"
									+ "blog\n"
									+ "show feed"
									+ "post <\"titolo\"> <\"contenuto\">\n"
									+ "show post <id_post>\n"
									+ "rewin <id_post>\n"
									+ "rate <id_post> <Voto> (esprimibile con +1 o -1)\n"
									+ "comment <id_post> <Comment>\n"
									+ "wallet\n"
									+ "wallet btc\n");
					break;
				case "register":
					// se sono loggato, non posso registrare nulla
					if (utente_client != null) {
						System.out.println("! sei già registrato");
						break;
					}
					// mi salvo il nome utente
					String nome_utente = tokenizer.nextToken();
					// mi salvo la password
					String password = tokenizer.nextToken();
					// mi salvo i tags
					ArrayList<String> tags = new ArrayList<String>();
					// recupero tutti i tags inseriti e li converto in minuscolo
					while (tokenizer.hasMoreTokens()) {
						tags.add(new String(tokenizer.nextToken()).toLowerCase());
					}
					// se i tags sono del numero sbagliato, do errore
					if (tags.size() == 0 || tags.size() > 5) {
						System.out.println("! inserire da uno a 5 tags");
						break;
					}
					try {
						// gestisco la risposta della chiamata rmi al server
						gestoreRispostaServer(server_rmi.register(nome_utente, password, tags));
					} catch (RemoteException e) {
						// errore, esco
						System.out.println("! errore fatale RMI");
						System.exit(-1);
					}
					break;

				case "exit":
					// flag = true
					exit = true;
					// se sono loggato, chiamo una logout
					if (utente_client != null) {
						return "logout";
					}
					break;

				case "login":
					// se sono gia' loggato, errore
					if (utente_client != null) {
						System.out.println("! sei già loggato");
						break;
					} else {
						// se non sono loggato, mi salvo il nome utente e spedisco la richiesta
						String nome_login = tokenizer.nextToken();
						utente_client = nome_login;

						return richiesta;
					}
				case "logout":
					// se sono gia' deloggato, errore
					if (utente_client == null) {
						System.out.println("! sei già deloggato");
						break;
					} else {
						// altrimenti ok
						return richiesta;
					}
				case "list":
					// se sono loggato procedo
					if (utente_client != null) {
						// se ho chiamato list followers chiamo RMI
						if (tokenizer.nextToken().equals("followers")) {
							// consulto la mia interfaccia caching rmi callback
							ArrayList<String> seguaci = client_rmi.getFollowers();
							// se ho seguaci li stampo
							if (!seguaci.isEmpty()) {
								System.out.println("<< " + seguaci.toString());
							} else {
								// altrimenti non ne ho
								System.out.println("<< non hai nessun seguace.");
							}
							break;
						}
					} else {
						// devi essere loggato
						System.out.println("! esegui il login prima di effettuare richieste");
						break;
					}

				default:
					// default
					return richiesta;
			}

		} catch (NoSuchElementException e) {
			// bad request
			System.err.println("richiesta mal costruita. ");

		}

		return null;
	}

	// gestisce la risposta del server
	public static void gestoreRispostaServer(Risposta risposta) {
		// se la risposta e' una login che ha avuto successo devo fare alcune cose
		if (risposta.getRisultato() == CodiciRitorno.LOGIN_SUCCESS) {
			// mi devo registrare per la callback
			try {
				server_rmi.registerForCallback(utente_client, client_callback);
			} catch (RemoteException e) {
				System.out.println("! errore fatale RMI");
				e.printStackTrace();
				System.exit(-1);
			}
			// mi devo salvare le credenziali multicast, sono solo in questa risposta
			indirizzo_multicast = risposta.getMulticastAddress();
			porta_multicast = risposta.getMulticastPort();
			// se non ci sono problemi mi connetto al multicast
			if (indirizzo_multicast != null && porta_multicast != -1) {
				try {
					// nuovo socket
					multicast_socket = new MulticastSocket(porta_multicast);
					// nuovo gruppo
					multicast_gruppo = new InetSocketAddress(InetAddress.getByName(indirizzo_multicast),
							porta_multicast);
					// nuova interfaccia di rete
					network_interface = NetworkInterface.getByName(configurazione.getNetInterface());
					// mi metto in ascolto
					multicast_socket.joinGroup(multicast_gruppo, network_interface);
				} catch (IOException e) {
					// errore fatale
					System.out.println("! errore fatale MULTICAST");
					System.exit(-1);
				}
				// creo un nuovo thread per l'ascolto e lo lancio
				ClientMulticastThread client_multicast = new ClientMulticastThread(multicast_socket, multicast_gruppo,
						network_interface);
				multicast_thread = new Thread(client_multicast);
				multicast_thread.start();
			} else {
				// errore, esco
				System.out.println("! errore fatale di comunicazione");
				System.exit(-1);
			}

		}
		// se la risposta e' una logout che ha avuto successo devo fare alcune cose
		else if (risposta.getRisultato() == CodiciRitorno.LOGOUT_SUCCESS) {

			// mi devo deregistrare per la callback del server
			try {
				server_rmi.unregisterForCallback(utente_client);
				// resetto variabile di logging
				utente_client = null;
			} catch (RemoteException e) {
				// errore,esco
				System.out.println("! errore fatale RMI");
				System.exit(-1);
			}
			// devo chiudere il thread multicast
			multicast_thread.interrupt();
			try {
				multicast_socket.leaveGroup(multicast_gruppo, network_interface);
				// chiudendo il socket, scateno un'eccezione nel thread che chiude tutto
				multicast_socket.close();
			} catch (IOException e) {
				// errore
				System.out.println("! errore fatale MULTICAST");
				System.exit(-1);
			}

		}
		// se la risposta e' una login andata in fallimento, devo resettare la variabile
		// di login globale
		else if (risposta.getRisultato() == CodiciRitorno.LOGIN_FAILURE) {
			utente_client = null;
		}
		// in ogni caso, stampo la stringa di risposta
		System.out.println("<< " + risposta.getRisposta());
	}
}
