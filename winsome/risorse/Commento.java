
//risorsa commento
public class Commento {
	//autore del commento
	String autore;
	//contenuto del commento
	String commento;

	
	//per la libreria jackson costruttore vuoto + metodi get e set per tutti gli attributi della classe

	public Commento() {
		
	}
	
	public Commento(String autore,String commento) {
		this.autore = autore;
		this.commento = commento;
	}
	
	public void setAutore(String autore) {
		this.autore = autore;
	}
	
	public void setCommento(String commento) {
		this.commento = commento;
	}
	
	public String getAutore() {
		return autore;
	}
	
	public String getCommento() {
		return commento;
	}
}
