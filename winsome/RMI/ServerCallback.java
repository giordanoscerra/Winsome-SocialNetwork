
import java.rmi.server.RemoteObject;
import java.util.ArrayList;

//classe RMI utilizzata dal client e dal server 
public class ServerCallback extends RemoteObject implements InterfacciaServerCallback {
    //lista di seguaci
    private ArrayList<String> seguaci;

    //costruttore
    ServerCallback() {
        this.seguaci = new ArrayList<String>();
    }

    //metodi chiamati dal SERVER per notificare al client la presenza di un nuovo seguace
    public synchronized void notificaFollow(String seguace) {
        //put if absent
        if (!seguaci.contains(seguace)) {
            seguaci.add(seguace);
            System.out.println("<< "+seguace+" ha appena iniziato a seguirti!");
        }
    }

    //metodi chiamati dal SERVER per notificare al client che qualcuno lo ha smesso di seguire
    public synchronized void notificaUnfollow(String nemico) {

        //remove if present
        if (seguaci.contains(nemico)) {
            seguaci.remove(nemico);
            System.out.println("<< "+nemico+" ha appena smesso di seguirti...");

        }
    }

    //metodo set chiamato dal SERVER
    public synchronized void setFollowers(ArrayList<String> seguaci) {
        this.seguaci = seguaci;
    }

    //metodo get chiamato dal CLIENT - ritorna una copia
    public synchronized ArrayList<String> getFollowers() {
        return new ArrayList<String>(seguaci);
    }
}
