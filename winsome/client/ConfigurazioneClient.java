//classe per la configurazione del client
public class ConfigurazioneClient {
    //indirizzo IP del server
    private String indirizzo_server;

    //porta tcp per la connessione con il server
    private int tcp_port;

    //porta per il registro RMI
    private int registry_port;

    //nome dell'oggetto RMI esposto
    private String oggetto_rmi;

    //interfaccia di rete utilizzata per la connessione multicast
    private String net_interface;

    //costruttore
    public ConfigurazioneClient(String indirizzo_server, int tcp_port, int registry_port, String oggetto_rmi, String net_interface) {
        this.indirizzo_server = indirizzo_server;
        this.tcp_port = tcp_port;
        this.registry_port = registry_port;
        this.oggetto_rmi = oggetto_rmi;
        this.net_interface = net_interface;
    }

    

    //per la libreria jackson costruttore vuoto + metodi get e set per tutti gli attributi della classe


    public ConfigurazioneClient() {
    }

    public String getIndirizzoServer() {
        return indirizzo_server;
    }

    public void setIndirizzoServer(String indirizzo_server) {
        this.indirizzo_server = indirizzo_server;
    }

    public int getTcpPort() {
        return tcp_port;
    }

    public void setTcpPort(int tcp_port) {
        this.tcp_port = tcp_port;
    }

    public int getRegistryPort() {
        return registry_port;
    }

    public void setRegistryPort(int registry_port) {
        this.registry_port = registry_port;
    }

    public String getOggettoRmi() {
        return oggetto_rmi;
    }

    public void setOggettoRmi(String oggetto_rmi) {
        this.oggetto_rmi = oggetto_rmi;
    }

    public String getNetInterface() {
        return net_interface;
    }

    public void setNetInterface(String net_interface) {
        this.net_interface = net_interface;
    }
}
