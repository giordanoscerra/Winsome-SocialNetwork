
import java.util.ArrayList;

public class Utente {
	//identificativo univoco dell'utente
	private String nome_utente;
	//password dell'utente, utilizzata per l'accesso
	private String password;
	//tags, gli interessi dell'utente
	private ArrayList<String> interessi;
	//seguaci
	private ArrayList<String> seguaci;
	//seguiti
	private ArrayList<String> seguiti;
	
	//per la libreria jackson costruttore vuoto + metodi get e set per tutti gli attributi della classe. 


	public Utente() {
	}

	public Utente(String nome_utente, String password, ArrayList<String> interessi) {
		this.nome_utente = nome_utente;
		this.password = password;
		this.interessi = interessi;
		this.seguaci = new ArrayList<String>();
		this.seguiti = new ArrayList<String>();
	}

	public void setNomeUtente(String nome_utente) {
		this.nome_utente = nome_utente;
	}
	
	public String getNomeUtente() {
		return this.nome_utente;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getPassword() {
		return this.password;
	}
	
	//senza una set: non sono modificabili
	public ArrayList<String> getInteressi() {
		return this.interessi;
	}

	//metodo sincronizzato utilizzato per aggiungere un seguace: equivale ad un PutIfAbsent
	public synchronized int aggiungiSeguace(String seguace) {
		if(seguaci.contains(seguace))
			return 0;
		else
			seguaci.add(seguace);
		return 1;
	}
	//metodo sincronizzato utilizzato per eliminare un seguace: equivale ad un RemoveIfPresent	
	public synchronized int rimuoviSeguace(String seguace) {
		if(!seguaci.contains(seguace))
			return 0;
		else
			seguaci.remove(seguace);
		return 1;
	}
	//metodo sincronizzato utilizzato per aggiungere un seguito: equivale ad un PutIfAbsent
	public synchronized int aggiungiSeguito(String seguito) {
		if(this.seguiti.contains(seguito))
			return 0;
		else
			this.seguiti.add(seguito);
		return 1;
	}
	
	//metodo sincronizzato utilizzato per eliminare un seguito: equivale ad un RemoveIfPresent	
	public synchronized int rimuoviSeguito(String seguito) {
		if(!this.seguiti.contains(seguito))
			return 0;
		else
			this.seguiti.remove(seguito);
		return 1;
	}
	
	//ritorno una copia della lista dei seguiti
	public synchronized ArrayList<String> getSeguiti(){
		return new ArrayList<>(seguiti);
	}
	//ritorno una copia della lista dei seguaci
		public ArrayList<String> getSeguaci(){
		return new ArrayList<>(seguaci);
	}
}
