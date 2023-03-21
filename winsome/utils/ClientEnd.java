//classe dove si mantiene status del client, la sua richiesta e la risposta del server
public class ClientEnd {
	//bool per notificare l'accesso
	public boolean accesso = false;
	//nome utente univoco dell'utente associato al client
	public String nome_utente = null;
	//stringa che contiene la richiesta del client
	public String richiesta = null;
	//risposta del server
	public Risposta risposta;
	
    //per la libreria jackson costruttore vuoto + metodi get e set per tutti gli attributi della classe

	public ClientEnd() {
	}
	
	public void setAccesso(boolean accesso) {
		this.accesso = accesso;
	}
	
	public void setNomeUtente(String nome_utente) {
		this.nome_utente = nome_utente;
	}
	
	public void setRichiesta(String richiesta) {
		this.richiesta = richiesta;
	}
	
	public void setRisposta(Risposta risposta) {
		this.risposta = risposta;
	}
	
	public boolean getAccesso() {
		return this.accesso;
	}
	
	public String getNomeUtente() {
		return this.nome_utente;
	}
	
	public String getRichiesta() {
		return this.richiesta;
	}
	
	public Risposta getRisposta() {
		return this.risposta;
	}

}