import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.HashMap;

//classe RMI utilizzata dal client e dal server 
public class RmiServer extends RemoteServer implements InterfacciaRmiServer {

    //riferimento al server winsome
    private WinsomeServer winsome;
    //mappa di client con le loro interfacce per la callback
    private HashMap<String, InterfacciaServerCallback> clients;

    //costruttore
    RmiServer(WinsomeServer winsome) {
        this.winsome = winsome;
        this.clients = new HashMap<String, InterfacciaServerCallback>();
    }
    @Override
    //il metodo register chiama semplicemente la register del server winsome
    public Risposta register(String utente, String password, ArrayList<String> tags) throws RemoteException {

        return winsome.register(utente, password, tags);
    }

	//dopo il login, il client si registra per le callback sulla lista seguaci
    public synchronized void registerForCallback(String utente, InterfacciaServerCallback ClientInterface)
            throws RemoteException {
        //put if absent
        if (!clients.containsKey(utente)) {
            clients.put(utente, ClientInterface);
            //richiedo al server la lista dei miei seguaci
            ClientInterface.setFollowers(winsome.getFollowers(utente));
        }
    }

	//dopo il logout, il client si deregistra per le callback sulla lista seguaci: non ricevera' piu' aggiornamenti
    public synchronized void unregisterForCallback(String utente)
            throws RemoteException {
        //remove if present
        if (clients.containsKey(utente)) {
            clients.remove(utente);
        }

    }
    //metodi chiamati dal SERVER per notificare al client la presenza di un nuovo seguace
    public synchronized void followCallback(String utente, String seguace) {
        //se l'utente e' registrato per la callback notifico
        if (clients.containsKey(utente)) {
            try {
                //notifico
                clients.get(utente).notificaFollow(seguace);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    //metodi chiamati dal SERVER per notificare al client che qualcuno lo ha smesso di seguire
    public synchronized void unfollowCallback(String utente, String nemico) {
        //se l'utente e' registrato per la callback notifico
        if (clients.containsKey(utente)) {
            try {
                //notifico
                clients.get(utente).notificaUnfollow(nemico);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

}
