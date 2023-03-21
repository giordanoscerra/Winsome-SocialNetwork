import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

//MAIN server
public class MainServer {

    //counter delle connessioni attive
    private static int connessioni_attive;
    //ritardo per la chiusura del threadpool
    private static int ritardo_chiusura = 60000;
    //coda blocking di thread del threadpool
    private static BlockingQueue<Runnable> coda = new ArrayBlockingQueue<Runnable>(100);
    //threadpool
    private static ExecutorService threadpool = null;
    //riferimento al server winsome
    private static WinsomeServer winsome = null;
    //mapper per file json
    private static ObjectMapper mapper = new ObjectMapper();
    //oggetto che contiene la configurazione
    private static ConfigurazioneServer configurazione = null;

    public static void main(String[] args) {
        //inizializzo
        connessioni_attive = 0;
        RmiServer rmi_server = null;
        //leggo il file di configurazione
        try {
            configurazione = mapper.readValue(Paths.get("json/JSONconfigurazioneserver.json").toFile(),
                    ConfigurazioneServer.class);
        } catch (IOException e1) {
            //errore fatale
            System.out.println("errore fatale nel parsing del file di CONFIGURAZIONE esco");
            // e1.printStackTrace();
            System.exit(-1);
        }
        //creo threadpool con numero workers massimo configurabile
        threadpool = new ThreadPoolExecutor(1, configurazione.getNumeroWorkers(), ritardo_chiusura,
                TimeUnit.MILLISECONDS, coda,
                new ThreadPoolExecutor.AbortPolicy());
        //creo un nuovo server winsome
        winsome = new WinsomeServer(configurazione);
        //creo connessione tcp
        try (ServerSocketChannel server_socket = ServerSocketChannel.open()) {
            server_socket.socket().bind(new InetSocketAddress(configurazione.getPortaTcp()));
            //setto modalita' non bloccante
            server_socket.configureBlocking(false);
            //creo un nuovo selettore
            Selector selettore = Selector.open();
            //mi registro per l'accept sul selettore: sono pronto ad accettare connessioni
            server_socket.register(selettore, SelectionKey.OP_ACCEPT);
            System.out.printf("Server: in attesa di connessioni sulla porta %d\n", configurazione.getPortaTcp());
            //creo oggetti RMI
            try {
                //creo oggetto RMI da esporre
                rmi_server = new RmiServer(winsome);
                //esporto l'oggetto
                InterfacciaRmiServer stub = (InterfacciaRmiServer) UnicastRemoteObject.exportObject(rmi_server, 0);
                //creo un registro sulla porta configurabile
                LocateRegistry.createRegistry(configurazione.getPortaRegistro());
                //lo recupero
                Registry r = LocateRegistry.getRegistry(configurazione.getPortaRegistro());
                //binding
                r.rebind(configurazione.getOggettoRmi(), stub);
                
            } catch (RemoteException e) {
                //errore fatale
                System.out.println("! errore fatale RMI ");
                System.exit(-1);
            }

            //recupero lo stato del server
            try {
                //leggo id post
                File backup_id_posts = new File("json/JSONidpost.json");
                if (backup_id_posts.exists() && !backup_id_posts.isDirectory()) {
                    winsome.setId_post(mapper.readValue(backup_id_posts, int.class));
                }
                //leggo mappa utenti
                File backup_utenti = new File("json/JSONutenti.json");
                if (backup_utenti.exists() && !backup_utenti.isDirectory()) {

                    winsome.setUtenti(
                            mapper.readValue(backup_utenti, new TypeReference<ConcurrentHashMap<String, Utente>>() {
                            }));
                }
                //leggo mappa blog
                File backup_posts = new File("json/JSONblog.json");
                if (backup_posts.exists() && !backup_posts.isDirectory()) {
                    winsome.setBlog(mapper.readValue(backup_posts,
                            new TypeReference<ConcurrentHashMap<String, ArrayList<Post>>>() {
                            }));
                }
                //leggo mappa portafogli
                File backup_portafogli = new File("json/JSONportafogli.json");
                if (backup_portafogli.exists() && !backup_portafogli.isDirectory()) {
                    winsome.setWallets(mapper.readValue(backup_portafogli,
                            new TypeReference<ConcurrentHashMap<String, Portafoglio>>() {
                            }));
                }

            } catch (IOException e) {
                //errore non fatale, fresh start 
                System.out.println("nessun backup preesistente.");
            }
            //lancio il thread di backup
            ServerBackup server_backup = new ServerBackup(winsome);
            Thread server_backup_thread = new Thread(server_backup);
            server_backup_thread.start();

            //lancio il thread multicast
            ServerMulticastThread multicast_server = new ServerMulticastThread(winsome);
            Thread server_multicast_thread = new Thread(multicast_server);
            server_multicast_thread.start();

            //ciclo infinito
            while (true) {
                //il server e' pronto. se legge 0 skippa al prossimo ciclo
                if (selettore.select() == 0)
                    continue;
                //recupero le chiavi dei canali pronti
                Set<SelectionKey> selectedKeys = selettore.selectedKeys();
                //creo un iteratore sulle chiavi
                Iterator<SelectionKey> it = selectedKeys.iterator();
                //scorro tutte le chiavi pronte
                while (it.hasNext()) {
                    SelectionKey chiave = it.next();
                    it.remove();
                    try { 
                        //accetto nuovi connessioni
                        if (chiave.isAcceptable()) {
                            //creo il socket 
                            ServerSocketChannel server = (ServerSocketChannel) chiave.channel();
                            //accetto la connessione
                            SocketChannel client = server.accept();
                            //setto modalita' non bloccante
                            client.configureBlocking(false);
                            System.out.println(
                                    "accettata nuova connessione dal client: " + client.getRemoteAddress());
                            System.out.printf("numero di connessioni aperte: %d\n", ++connessioni_attive);
                            //nuovo status client
                            ClientEnd client_end = new ClientEnd();
                            //mi registro per la lettura con attachment client_end
                            client.register(selettore, SelectionKey.OP_READ, client_end);


                        } 
                        //se c'e' qualcosa da leggere sul canale associato alla chiave
                        else if (chiave.isReadable()) {
                            //recupero il socket
                            SocketChannel client = (SocketChannel) chiave.channel();
                            //recupero l'attachment
                            ClientEnd client_end = (ClientEnd) chiave.attachment();
                            //preparo un buffer di lettura
                            ByteBuffer buffer_richiesta = ByteBuffer.allocate(1024);
                            //lo pulisco per scriverci
                            buffer_richiesta.clear();
                            //leggo in modalita' non bloccante
                            int byte_letti = client.read(buffer_richiesta);
                            //modalita' lettura
                            buffer_richiesta.flip();
                            //se ho appena iniziato a a leggere
                            if (client_end.richiesta == null)
                                //creo una nuova stringa con il contenuto del buffer
                                client_end.richiesta = new String(buffer_richiesta.array()).trim();
                            else {
                                //altrimenti, appendo
                                client_end.richiesta = client_end.richiesta
                                        + new String(buffer_richiesta.array()).trim();
                            }
                            //se ho riempito il buffer, non significa che ho letto tutto
                            if (byte_letti == 1024) {
                                chiave.attach(client_end);
                            } 
                            //se ho letto -1, la connessione e' caduta , chiudo e cancello
                            else if (byte_letti == -1) {
                                chiave.cancel();
                                chiave.channel().close();
                                System.out.printf("numero di connessioni aperte: %d\n", --connessioni_attive);
                            } 
                            //se non ho riempito il buffer , ho sicuramente letto tutto!
                            else if (byte_letti < 1024) {
                                //posso passare la richiesta al thread handler
                                //mi deregistro momentaneamente dal selettore
                                chiave.interestOps(0);
                                threadpool.execute(new WorkerThread(chiave, winsome, rmi_server));
                                buffer_richiesta.clear();

                            }

                        } 
                        //sono pronto a rispondere al client
                        else if (chiave.isWritable()) {
                            //recupero il socket
                            SocketChannel client = (SocketChannel) chiave.channel();
                            //modalita' non bloccante
                            client.configureBlocking(false);
                            //recupero lo status del client
                            ClientEnd client_end = (ClientEnd) chiave.attachment();
                            //trasformo la risposta in una stringa e la metto nel campo richiesta del client_end
                            client_end.richiesta = mapper.writeValueAsString(client_end.risposta);
                            //se c'e' stato un problema chiudo e cancello
                            if (client_end.richiesta == null) {
                                System.err.println("WORKER: Errore con il client, chiudo la connessione");
                                chiave.cancel();
                                client.close();
                            }
                            //preparo un buffer di risposta e lo scrivo sul socket del client
                            ByteBuffer buffer_risposta = ByteBuffer.wrap(client_end.richiesta.getBytes());
                            int byte_scritti = client.write(buffer_risposta);
                            //se ho scritto tutto mi riregistro in lettura sul selettore
                            if (byte_scritti == client_end.richiesta.getBytes().length) {
                                //resetto i campi del client status, per non interferire sulle prossime richieste
                                client_end.richiesta = null;
                                client_end.risposta = null;
                                chiave.attach(client_end); 
                                chiave.interestOps(SelectionKey.OP_READ);
                            }
                        }
                    } 
                    //se nel frattempo il client va giu', devo fare un paio di cose
                    catch (IOException e) { 
                        System.out.println("SERVER MAIN -> Terminazione improvvisa del client");
                        System.out.printf("numero di connessioni aperte: %d\n", --connessioni_attive);
                        ClientEnd client_end = (ClientEnd) chiave.attachment();
                        //mi deregistro per la callback sul server
                        if (client_end.nome_utente != null) {
                            rmi_server.unregisterForCallback(client_end.nome_utente);
                        }
                        //effettuo il logout
                        winsome.logout(client_end.nome_utente);
                        //cancello la chiave
                        chiave.channel().close();
                        chiave.cancel();
                    }
                }
            }
        } catch (IOException e) {
            //errore fatale
            System.out.println(" errore fatale ");
            System.exit(-1);
        }
    }

}
