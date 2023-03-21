
import java.rmi.*;
import java.util.ArrayList;

//interfaccia esposta al client, questi sono i metodi implementati lato server che il client puo' chiamare via RMI
public interface InterfacciaRmiServer extends Remote{
	//metodo per registrare un utente
	public Risposta register(String username, String password, ArrayList<String> tags) throws RemoteException;
	//dopo il login, il client si registra per le callback sulla lista seguaci
	public void registerForCallback(String username, InterfacciaServerCallback ClientInterface) throws RemoteException;
	//dopo il logout, il client si deregistra per le callback sulla lista seguaci: non ricevera' piu' aggiornamenti
	public void unregisterForCallback(String username) throws RemoteException;
}
