import java.rmi.*;
import java.util.ArrayList;

//interfaccia esposta dal client, utilizzata dal server per comunicare al client i nuovi seguaci, o i vecchi
public interface InterfacciaServerCallback extends Remote{
    //il server notifica al client che e' appena stato seguito
    public void notificaFollow(String seguace) throws RemoteException;
    //il server notifica al client che lo hanno appena smesso di seguire
    public void notificaUnfollow(String nemico) throws RemoteException;
    //metodo utilizzato dal server per settare la lista dei seguaci di un utente
    public void setFollowers(ArrayList<String> seguaci) throws RemoteException;
    
}
