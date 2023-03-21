

import java.util.ArrayList;

//risorsa post, effettua l'override del metodo compareTo quindi implementa l'interfaccia Comparable
public class Post implements Comparable<Post> {
	//id univoco del post
	private int id;
	//titolo del post
	private String titolo;
	//contenuto del post
	private String contenuto;
	//autore del post
	private String autore;
	//se il post e' stato rewinnato mi segno l'autore originale
	private String rewinned;
	//numero delle iterazioni per la premiazione: piu' e' alto, meno importante sara' la premiazione
	private int it_premiazioni;
	//strutture dati che mi permettono di calcolare la quota da distribuire ai curatori
	//si dividono in premiati e da premiare: significa semplicemente che sono dati significativi per il calcolo oppure no
	//infatti, tecnicamente i dislikes non si premiano, quindi forse sarebbe stato piu' giusto chiamarli computati e non
	ArrayList<String> likes_premiati = new ArrayList<String>();
	ArrayList<String> likes_da_premiare = new ArrayList<String>();
	ArrayList<String> dislikes_premiati = new ArrayList<String>();
	ArrayList<String> dislikes_da_premiare = new ArrayList<String>();
	ArrayList<Commento> commenti_premiati = new ArrayList<Commento>();
	ArrayList<Commento> commenti_da_premiare = new ArrayList<Commento>();
	
	//per la libreria jackson costruttore vuoto + metodi get e set per tutti gli attributi della classe. 

	Post() {
	}
	
	public Post(String titolo, String contenuto, String autore, int id, int it_premiazioni) {
		this.id = id;
		this.titolo = titolo;
		this.contenuto = contenuto;
		this.autore = autore;
		this.it_premiazioni = it_premiazioni;
		this.rewinned = null;
	}
	//ulteriore costruttore, utilizzato per il rewin
	public Post(String titolo, String contenuto, String autore, int id, int it_premiazioni, String rewinned) {
		this.id = id;
		this.titolo = titolo;
		this.contenuto = contenuto;
		this.autore = autore;
		this.it_premiazioni = it_premiazioni;
		this.rewinned = rewinned;
	}

	//metodo sincronizzato per aggiungere mi piace al post
	public synchronized int miPiace(String utente) {
		//controlla se l'utente ha gia' valutato il post
		if(likes_da_premiare.contains(utente) || likes_premiati.contains(utente) || dislikes_da_premiare.contains(utente) || dislikes_premiati.contains(utente))
			//se si, errore
			return 0;
		else {
			//altrimenti aggiunge il like
			likes_da_premiare.add(utente);
		}
		return 1;
	}
	//metodo sincronizzato per aggiungere non mi piace al post
	public synchronized int nonMiPiace(String utente) {
		//controlla se l'utente ha gia' valutato il post
		if(dislikes_da_premiare.contains(utente) || dislikes_premiati.contains(utente) || likes_da_premiare.contains(utente) || likes_premiati.contains(utente))
		//se si, errore
		return 0;
		else {
			//altrimenti aggiunge il dislike
			dislikes_da_premiare.add(utente);
		}
		return 1;
	}
	
	//metodo sincronizzato per aggiungere un commento al post: non ci sono vincoli
	//se non che l'autore non puo' commentare un proprio post: e' controllato lato server
	public synchronized void commenta(Commento commento) {
		commenti_da_premiare.add(commento);
	}
	
	
	public int getId() {
		return this.id;
	}
	
	public String getTitolo() {
		return this.titolo;
	}
	
	public String getContenuto() {
		return this.contenuto;
	}

	public String getAutore() {
		return this.autore;
	}
	
	public String getRewinned() {
		return this.rewinned;
	}

	public int getItPremiazioni() {
		return this.it_premiazioni;
	}
	
	public ArrayList<String> getLikesPremiati() {
		return this.likes_premiati;
	}
	
	public ArrayList<String> getLikesDaPremiare() {
		return this.likes_da_premiare;
	}
	
	public ArrayList<String> getDislikesPremiati() {
		return this.dislikes_premiati;
	}
	
	public ArrayList<String> getDislikesDaPremiare() {
		return this.dislikes_da_premiare;
	}
	
	public ArrayList<Commento> getCommentiPremiati() {
		return this.commenti_premiati;
	}
	
	public ArrayList<Commento> getCommentiDaPremiare() {
		return this.commenti_da_premiare;
	}
	
	//ritorna il numero totale di mi piace del post
	public synchronized int numeroMiPiace() {
		return likes_da_premiare.size() + likes_premiati.size();
	}
	//ritorna il numero totale di non mi piace del post
	public synchronized int numeroNonMiPiace() {
		return dislikes_da_premiare.size() + dislikes_premiati.size();
	}
	
	//ritorna l'intera lista dei commenti del post
	public synchronized ArrayList<Commento> listaCommenti(){
		ArrayList<Commento> comments = new ArrayList<Commento>();
		comments.addAll(commenti_da_premiare);
		comments.addAll(commenti_premiati);
		return comments;
	}
	
	//ritorna il numero di commenti di un certo utente: 
	//utilizzato nel calcolo delle ricompense
	public synchronized int numeroCommentiDi(String utente) {
		
		if(commenti_premiati.isEmpty() && commenti_da_premiare.isEmpty() )
			return 0;
		
		int totale = 0;
		
		for(Commento commento : commenti_premiati) {
			if(commento.getAutore().equals(utente))
				totale++;
		}
		
		for(Commento commento : commenti_da_premiare) {
			if(commento.getAutore().equals(utente))
				totale++;
		}
		
		return totale;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public void setTitolo(String titolo) {
		this.titolo = titolo;
	}
	
	public void setContenuto(String contenuto) {
		this.contenuto = contenuto;
	}
	
	public void setAutore(String autore) {
		this.autore = autore;
	}
	
	public void setRewinned(String rewinned) {
		this.rewinned = rewinned;
	}
	
	public void setItPremiazioni(int it_premiazioni) {
		this.it_premiazioni = it_premiazioni;
	}
	
	public void setLikesPremiati(ArrayList<String> likes_premiati) {
		this.likes_premiati = likes_premiati;
	}
	
	public void setLikesDaPremiare(ArrayList<String> likes_da_premiare) {
		this.likes_da_premiare = likes_da_premiare;
	}
	
	public void setDislikesPremiati(ArrayList<String> dislikes_premiati) {
		this.dislikes_premiati = dislikes_premiati;
	}
	
	public void setDislikesDaPremiare(ArrayList<String> dislikes_da_premiare) {
		this.dislikes_da_premiare = dislikes_da_premiare;
	}
	
	public void setCommentiPremiati(ArrayList<Commento> commenti_premiati) {
		this.commenti_premiati = commenti_premiati;
	}
	
	public void setCommentiDaPremiare(ArrayList<Commento> commenti_da_premiare) {
		this.commenti_da_premiare = commenti_da_premiare;
	}
	
	//metodo overrided per ordinare la lista dei post
	@Override
	public int compareTo(Post o) {
		if(this.id < o.id) {
			return 1;
		}
		else {
			return -1;
		}
		
	}
}
