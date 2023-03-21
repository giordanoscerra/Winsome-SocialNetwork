
//classe thread per la consistenza del server
public class ServerBackup implements Runnable {
	//mantiene un riferimento al server winsome
	WinsomeServer winsome;
	//costruttore
	ServerBackup(WinsomeServer winsome) {
		this.winsome = winsome;
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				//dorme per tot secondi, leggendo dal file di configurazione
				Thread.sleep(winsome.configurazione.getTimerBackup());
			} catch (InterruptedException e) {
					System.out.println("il thread backup e' stato interrotto.");
					//esco, sono stato interrotto
					return;
			}
			//chiamo il metodo di winsome che mi consente di effettuare il backup
			winsome.salva_stato_server();
			//stampa
			System.out.println("\tserver salvato");
		}
	}
}