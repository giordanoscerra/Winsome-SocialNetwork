
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

//risorsa portafoglio : ogni utente registrato ne ha uno
public class Portafoglio {

	//proprietario del portafoglio
	private String proprietario;
	//valuta totale posseduta dal proprietario
	private double totale_wincoins;
	//storico delle transazioni effettuate verso il proprietario
	private ArrayList<String> transazioni;


	//per la libreria jackson costruttore vuoto + metodi get e set per tutti gli attributi della classe. 
	//sono sincronizzati poiche' il portafoglio e' una risorsa condivisa

	public Portafoglio() {
	}
	
	public Portafoglio(String proprietario) {
		this.proprietario = proprietario;
		this.totale_wincoins = 0;
		this.transazioni = new ArrayList<String>();
	}

	//metodo sincronizzato per aggiungere wincoins al portafoglio e aggiornare le transazioni
	public synchronized void aggiungiWincoin(double coins) {
		
		//se la cifra e' significativa
		if (coins > 0) {
			//ricavo solo le due prime cifre decimali
			BigDecimal bd = BigDecimal.valueOf(coins);
			bd = bd.setScale(2, RoundingMode.HALF_UP);
			double wincoins = bd.doubleValue();
			//aggiorno l'attributo 
			this.totale_wincoins += wincoins;
			//preparo la data di questo momento
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			//aggiordno la struttura delle transizioni
			this.transazioni.add(dtf.format(LocalDateTime.now()) + " +" + wincoins + " wincoins");
		}
	}
	
	public synchronized String getProprietario() {
		return this.proprietario;
	}
	
	public synchronized double getTotaleWincoins() {
		BigDecimal bd = BigDecimal.valueOf(totale_wincoins);
		bd = bd.setScale(2, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}
	
	public synchronized ArrayList<String> getTransazioni(){
		return this.transazioni;
	}
	
	public synchronized void setProprietario(String proprietario) {
		this.proprietario = proprietario;
	}
	
	public synchronized void setTotaleWincoins(double totale_wincoins) {
		this.totale_wincoins = totale_wincoins;
	}
	
	public synchronized void setTransazioni(ArrayList<String> transazioni){
		this.transazioni = transazioni;
	}
}
