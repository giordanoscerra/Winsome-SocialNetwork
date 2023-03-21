//classe utilizzata dal server per rispondere alle richieste del client
public class Risposta implements java.io.Serializable {
	//risultato dell'operazione, tipicamente positivo, negativo o bad request
	private CodiciRitorno risultato;
	//stringa con una risposta anche di errore
	private String risposta;
	//campi per il multicast, passati dal server al client nel momento del login
	private String multicastAddress = null;
	private int multicastPort = -1;

	
    //per la libreria jackson costruttore vuoto + metodi get e set per tutti gli attributi della classe

	
	public Risposta() {
	}
	
	public Risposta(CodiciRitorno risultato, String risposta) {
		this.risultato = risultato;
		this.risposta = risposta;

	}

	public Risposta(CodiciRitorno risultato, String risposta, String multicastAddress, int multicastPort) {
		this.risultato = risultato;
		this.risposta = risposta;
		this.multicastAddress = multicastAddress;
		this.multicastPort = multicastPort;
	}
	
	public void setRisultato(CodiciRitorno risultato) {
		this.risultato = risultato;
	}
	
	public void setRisposta(String risposta) {
		this.risposta = risposta;
	}
	
	public CodiciRitorno getRisultato() {
		return this.risultato;
	}
	
	public String getRisposta() {
		return this.risposta;
	}

	public String getMulticastAddress() {
		return this.multicastAddress;
	}
	
	public int getMulticastPort() {
		return this.multicastPort;
	}

	public void setMulticastAddress(String multicastAddress) {
		this.multicastAddress = multicastAddress;
	}
	
	public void setMulticastPort(int multicastPort) {
		this.multicastPort = multicastPort;
	}
}
