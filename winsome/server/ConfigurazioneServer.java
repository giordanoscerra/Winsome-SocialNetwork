//classe per la configurazione del server
public class ConfigurazioneServer {

    //porta udp
    private int porta_udp;

    //porta tcp
    private int porta_tcp;

    //indirizzo multicast 
    private String indirizzo_multicast;

    //porta multicast
    private int porta_multicast;

    //porta registro
    private int porta_registro;

    //nome dell'oggetto RMI esposto
    private String oggetto_rmi;

    //timer per il thread delle premiazioni
    private int timer_premiazioni;

    //timer per il thread del backup
    private int timer_backup;

    //numero massimo di workers per il mio threadpool
    private int numero_workers;

    //percentuale ricompensa autore
    private float percentuale_autore;


    //per la libreria jackson costruttore vuoto + metodi get e set per tutti gli attributi della classe

    public ConfigurazioneServer(){
    }

    public ConfigurazioneServer(int porta_udp, int porta_tcp, String indirizzo_multicast, int porta_multicast, int porta_registro, String oggetto_rmi, int timer_premiazioni, int timer_backup, int numero_workers, float percentuale_autore) {
        this.porta_udp = porta_udp;
        this.porta_tcp = porta_tcp;
        this.indirizzo_multicast = indirizzo_multicast;
        this.porta_multicast = porta_multicast;
        this.porta_registro = porta_registro;
        this.oggetto_rmi = oggetto_rmi;
        this.timer_premiazioni = timer_premiazioni;
        this.timer_backup = timer_backup;
        this.numero_workers = numero_workers;
        this.percentuale_autore = percentuale_autore;
    }

    public int getPortaUdp() {
        return porta_udp;
    }

    public void setPortaUdp(int porta_udp) {
        this.porta_udp = porta_udp;
    }

    public int getPortaTcp() {
        return porta_tcp;
    }

    public void setPortaTcp(int porta_tcp) {
        this.porta_tcp = porta_tcp;
    }

    public String getIndirizzoMulticast() {
        return indirizzo_multicast;
    }

    public void setIndirizzoMulticast(String indirizzo_multicast) {
        this.indirizzo_multicast = indirizzo_multicast;
    }

    public int getPortaMulticast() {
        return porta_multicast;
    }

    public void setPortaMulticast(int porta_multicast) {
        this.porta_multicast = porta_multicast;
    }

    public int getPortaRegistro() {
        return porta_registro;
    }

    public void setPortaRegistro(int porta_registro) {
        this.porta_registro = porta_registro;
    }

    public String getOggettoRmi() {
        return oggetto_rmi;
    }

    public void setOggettoRmi(String oggetto_rmi) {
        this.oggetto_rmi = oggetto_rmi;
    }

    public int getTimerPremiazioni() {
        return timer_premiazioni;
    }

    public void setTimerPremiazioni(int timer_premiazioni) {
        this.timer_premiazioni = timer_premiazioni;
    }

    public int getTimerBackup() {
        return timer_backup;
    }

    public void setTimerBackup(int timer_backup) {
        this.timer_backup = timer_backup;
    }

    public int getNumeroWorkers() {
        return numero_workers;
    }

    public void setNumeroWorkers(int numero_workers) {
        this.numero_workers = numero_workers;
    }

    public float getPercentualeAutore() {
        return percentuale_autore;
    }

    public void setPercentualeAutore(float percentuale_autore) {
        this.percentuale_autore = percentuale_autore;
    }
}
