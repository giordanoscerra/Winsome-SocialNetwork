
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

//classe winsome server, contiene tutti i metodi richiesti
public class WinsomeServer {

    // oggetto per recuperare i parametri di configurazione del server
    ConfigurazioneServer configurazione = new ConfigurazioneServer();
    // object mapper per creare i file json delle classi java
    ObjectMapper mapper = new ObjectMapper();
    // contatore dei post, assegna atomicamente in maniera thread-safe ad ogni post
    // un id univoco
    private AtomicInteger id_post = new AtomicInteger(0);
    // concurrentHashMap thread-safe che memorizza tutti gli utenti registrati al
    // server : <nome utente-profilo utente>
    private ConcurrentHashMap<String, Utente> database_utenti = new ConcurrentHashMap<String, Utente>();
    // concurrentHashMap thread-safe che memorizza tutti i post pubblicati dagli
    // utenti : <nome utente, blog>
    private ConcurrentHashMap<String, ArrayList<Post>> database_blog = new ConcurrentHashMap<String, ArrayList<Post>>();
    // concurrentHashMap thread-safe che memorizza tutti i portafogli degli utenti
    // registrati : <nome utente, portafoglio>
    private ConcurrentHashMap<String, Portafoglio> database_portafogli = new ConcurrentHashMap<String, Portafoglio>();
    // hash set thread safe che mantiene la lista degli utenti attualmente loggati
    // nel server
    private ThreadSafeHashSet<String> utenti_loggati = new ThreadSafeHashSet<>();

    // costruttore
    public WinsomeServer(ConfigurazioneServer configurazione) {
        this.configurazione = configurazione;
    }

    // metodi get e set per il backup e la lettura da e su file json di backup

    public void setId_post(int idPost) {
        this.id_post.set(idPost);
    }

    public void setUtenti(ConcurrentHashMap<String, Utente> database_utenti) {
        this.database_utenti = database_utenti;
    }

    public void setBlog(ConcurrentHashMap<String, ArrayList<Post>> database_blog) {
        this.database_blog = database_blog;
    }

    public void setWallets(ConcurrentHashMap<String, Portafoglio> database_portafogli) {
        this.database_portafogli = database_portafogli;
    }

    public int getId_post() {
        return id_post.get();
    }

    // metodo login: l'utente vuole accedere al social network
    public Risposta login(String nome_utente, String password) {
        // acquisisco la lock sulla struttura che mantiene gli utenti
        synchronized (database_utenti) {
            // controllo se l'utente e' registrato
            if (!database_utenti.containsKey(nome_utente)) {
                // se non e' registrato ritorno una risposta con fallimento
                return new Risposta(CodiciRitorno.LOGIN_FAILURE, "utente non registrato");
            }
            // se l'utente e' registrato allora controllo se la password e' corretta
            if (password.equals(database_utenti.get(nome_utente).getPassword())) {
                // se la password e' corretta controllo se l'utente non fosse gia' loggato
                // l'accesso a quest'altra struttura dati e' controllato internamente alla
                // classe con read e write lock
                if (!utenti_loggati.contains(nome_utente)) {
                    // se va tutto bene posso aggiungere l'utente alla lista degli utenti loggati
                    utenti_loggati.add(nome_utente);
                    // ritorno una risposta con successo
                    // questo e' l'unica chiamata con costruttore secondario alla classe risposta
                    // passo al momento del login le credenziali per accedere al servizio di
                    // multicast del server
                    return new Risposta(CodiciRitorno.LOGIN_SUCCESS, "loggato con successo",
                            configurazione.getIndirizzoMulticast(),
                            configurazione.getPortaMulticast());
                } else {
                    // se l'utente e' gia' loggato ritorno una risposta con fallimento
                    return new Risposta(CodiciRitorno.LOGIN_FAILURE, "l'utente " + nome_utente + " è già loggato");
                }

            } else {
                // se la password e' sbagliata ritorno una risposta con fallimento
                return new Risposta(CodiciRitorno.LOGIN_FAILURE, "password errata");
            }
        }

    }

    // metodo logout: l'utente vuole scollegarsi dal social network
    public Risposta logout(String nome_utente) {
        // acquisisco la lock sulla struttura che mantiene gli utenti
        synchronized (database_utenti) {
            // controllo se l'utente esiste
            if (!database_utenti.containsKey(nome_utente)) {
                // se l'utente non esiste ritorno una risposta con fallimento
                return new Risposta(CodiciRitorno.LOGOUT_FAILURE, "utente non esistente.");
            }
            // controllo se l'utente non fosse loggato: auspicabilmente questo if dovrebbe
            // sempre dare false
            if (!utenti_loggati.contains(nome_utente)) {
                // se l'utente non era loggato ritorno fallimento
                return new Risposta(CodiciRitorno.LOGOUT_FAILURE,
                        "utente non loggato. questo non sarebbe dovuto succedere");
            } else {
                // se tutto va bene allora rimuovo in maniera thread safe l'utente dalla lista
                // dei loggati
                utenti_loggati.remove(nome_utente);
                // e ritorno con successo
                return new Risposta(CodiciRitorno.LOGOUT_SUCCESS, "deloggato con successo");
            }

        }

    }

    // metodo showFeed : mostra tutti i post degli utenti che seguo in ordine
    public Risposta showFeed(String nome_utente) {
        // controllo in maniera thread safe se ho dei seguiti da poter visualizzare
        if (database_utenti.get(nome_utente).getSeguiti().isEmpty()) {
            // se non seguo nessuno ritorno fallimento
            return new Risposta(CodiciRitorno.SHOWFEED_FAILURE, "non segui nessuno, feed vuoto");
        } else {
            // se ho dei seguiti acquisisco la lock sul database dei blog
            synchronized (database_blog) {
                // preparo la stringa di risposta
                StringBuilder risposta = new StringBuilder("\nId\t" +"|Titolo\t" +"|Autore\n");
                // prepapro un arrayList di post da riempire
                ArrayList<Post> feed = new ArrayList<>();
                // per ogni mio seguito recupero il loro blog e lo salvo nel feed
                for (String followed : database_utenti.get(nome_utente).getSeguiti()) {
                    //se quell'utente ha un blog lo aggiungo
                    if (database_blog.containsKey(followed)) {
                        feed.addAll(database_blog.get(followed));
                    }
                }
                // ordino i post che sono "per autore" in maniera decrescente secondo il loro id
                // (e quindi cronologicamente)
                Collections.sort(feed);
                // scorro ogni post del feed
                for (Post post : feed) {
                    // se il post non e' un rewin formatto l'output normalmente
                    if (post.getRewinned() == null) {
                        risposta.append(post.getId() + "\t|" +
                                post.getTitolo() + "\t|"
                                + post.getAutore() + "\n");
                    }
                    // se il post e' un rewin formatto l'output aggiungendo la dicitura "rewinnato
                    // da"
                    else {
                        risposta.append(post.getId() + "\t|" +
                                post.getTitolo() + "\t|"
                                + post.getAutore() +" (rewinnato da: " + post.getRewinned() + ")\n");
                    }
                }
                // se sono arrivato fin qui ritorno con successo il feed
                return new Risposta(CodiciRitorno.SHOWFEED_SUCCESS, risposta.toString());
            }
        }
    }

    // metodo listUsers : elenco tutti gli utenti registrati che hanno almeno un
    // interesse comune con me
    public Risposta listUsers(String nome_utente) {
        // preparo la stringa da ritornare come risposta
        StringBuilder risposta = new StringBuilder("\nlista utenti in comune:");
        // acquisisco la lock sul database utenti
        synchronized (database_utenti) {
            // recupero il mio profilo utente
            Utente utente = database_utenti.get(nome_utente);
            // scorro tutti gli utenti registrati
            for (String registrato : database_utenti.keySet()) {
                // non considero il mio caso banale
                if (registrato.equals(nome_utente))
                    continue;
                // recupero gli interessi dell'utente per poterli comparare con i miei
                ArrayList<String> tags = database_utenti.get(registrato).getInteressi();
                // flag per il formato risposta
                int nome = 0;
                // scorro tutti gli interessi
                for (String i : tags) {
                    // se ho almeno un interesse in comune con l'utente in questione mi salvo il suo
                    // nome
                    if (utente.getInteressi().contains(i)) {
                        // se non ho ancora scritto chi e', lo scrivo
                        if (nome == 0) {
                            risposta.append("\n" + registrato + " | ");
                            nome = 1;
                        }
                        // dopodiche' copio tutti gli interessi in comune con lui
                        risposta.append(i + " ");
                    }
                }

            }
        }
        // se la mia stringa iniziale e' stata modificata ritorno con successo la
        // risposta
        if (!risposta.toString().equals("\nlista utenti in comune:")) {
            return new Risposta(CodiciRitorno.LISTUSERS_SUCCESS, risposta.toString());
        }
        // altrimenti ritorno con fallimento: solo a me interessano queste cose
        else {
            return new Risposta(CodiciRitorno.LISTUSERS_FAILURE, "non hai interessi comuni a nessun altro utente");
        }
    }

    // metodo listFollowing: ritorna la lista degli utenti che seguo
    public Risposta listFollowing(String nome_utente) {
        // recupero in maniera thread-safe il mio profilo utente
        Utente utente = database_utenti.get(nome_utente);
        // preparo la stringa di risposta
        StringBuilder risposta = new StringBuilder("\necco i tuoi seguiti:\n");
        // recupero in maniera thread-safe una copia dei miei seguiti
        ArrayList<String> seguiti = utente.getSeguiti();
        // se la lista e' vuota, non seguo nessuno - ritorno con fallimento
        if (seguiti.isEmpty()) {
            return new Risposta(CodiciRitorno.LISTFOLLOWING_FAILURE, "non segui nessuno !");
        }
        // altrimenti, scorro tutta la lista e salvo i nomi
        for (String string : seguiti)
            risposta.append(string + "\n");
        // ritorno con successo i nomi degli utenti che seguo
        return new Risposta(CodiciRitorno.LISTFOLLOWING_SUCCESS, risposta.toString());
    }

    // metodo register : utilizzato per registrare un utente nel servizio
    public Risposta register(String nome_utente, String password, ArrayList<String> tags) {
        // preparo una stringa per accumulare gli errori che potrebbero scatenarsi
        StringBuilder errori = new StringBuilder();
        // flag per l'esito, se incontro un errore l'output sara' negativo
        int esito = 1;
        // se il nome utente passato e' la stringa vuota o semplicemente vuoto accumulo
        // un errore
        if (nome_utente.isBlank() || nome_utente.isEmpty()) {
            errori.append("nome_utente non valido\n");
            esito = 0;
        }
        // se i tags sono piu' di 5 (teoricamente gia' impossibile) o sono vuoti
        // accumulo un errore
        if (tags.isEmpty() || tags.size() > 5) {
            errori.append("Inserire da uno a cinque tag\n");
            esito = 0;
        }
        // controllo se avessi tags duplicati tramite il set che non ne ammette
        Set<String> set = new HashSet<String>(tags);
        // se ho duplicati accumulo un errore
        if (set.size() < tags.size()) {
            errori.append("Non sono ammessi tag duplicati\n");
            esito = 0;
        }
        // acquisisco la lock sul database_utenti
        synchronized (database_utenti) {
            // se l'utente e' gia' registrato accumulo un errore
            if (database_utenti.containsKey(nome_utente)) {
                errori.append("Nome utente già in uso\n");
                esito = 0;
            }
            // finalmente, controllo se c'e' stato almeno un errore
            if (esito != 0) {
                // se e' andato tutto bene posso procedere con la registrazione effettiva
                // trasformo le eventuali maiuscole in minuscole
                tags.replaceAll(String::toLowerCase);
                // creo il profilo del nuovo utente
                Utente nuovo_utente = new Utente(nome_utente, password, tags);
                // lo aggiungo al database - non ci sono problemi di concorrenza perche' ho
                // effettuato la lock
                database_utenti.put(nome_utente, nuovo_utente);
                // aggiungo un nuovo portafoglio al database portafogli - non ci sono problemi
                // di concorrenza
                // poiche' la put e' garantita essere atomica dalla concurrenthashmap
                // inoltre, la risorsa non poteva esistere prima di questo momento.
                database_portafogli.put(nome_utente, new Portafoglio(nome_utente));
                // ritorno con successo in maniera simpatica
                return new Risposta(CodiciRitorno.REGISTER_SUCCESS,
                        "Registrato! benvenuto su WINSOME carissimo " + nome_utente);
            }
        }
        // se invece ho riscontrato almeno un errore allora ritorno con fallimento
        // l'/gli errore
        return new Risposta(CodiciRitorno.REGISTER_FAILURE, errori.toString());
    }

    // metodo follow : utilizzato per seguire un altro utente registrato
    public Risposta follow(String nome_utente, String da_seguire) {
        // controllo se sto cercando di seguire me stesso, se si
        if (da_seguire.equals(nome_utente)) {
            // ritorno errore
            return new Risposta(CodiciRitorno.FOLLOW_FAILURE, "non puoi seguire te stesso !");
        }
        // se no posso procedere acquisendo la lock del database utenti
        synchronized (database_utenti) {
            // controllo se l'utente che voglio seguire sia registrato
            if (!database_utenti.containsKey(da_seguire)) {
                // se non lo e' ritorno con fallimento
                return new Risposta(CodiciRitorno.FOLLOW_FAILURE, "non esiste nessun " + da_seguire);
            }
            // se e' registrato posso procedere
            else {
                // la chiamata aggiungiSeguace ritornera' 1 se non seguivo gia' quella persona
                if (database_utenti.get(da_seguire).aggiungiSeguace(nome_utente) == 0) {
                    // in caso contrario ritorno con fallimento
                    return new Risposta(CodiciRitorno.FOLLOW_FAILURE, "segui già l'utente " + da_seguire);
                }
                // la chiamata aggiungiSeguito ritornera' 1 se quella persona non mi seguiva
                // gia'
                if (database_utenti.get(nome_utente).aggiungiSeguito(da_seguire) == 0) {
                    // in caso contrario ritorno con fallimento
                    return new Risposta(CodiciRitorno.FOLLOW_FAILURE, da_seguire + "segue già l'utente " + nome_utente);
                }
                // se e' andato tutto bene ritorno con fallimento
                return new Risposta(CodiciRitorno.FOLLOW_SUCCESS, "ora sei un follower di " + da_seguire);

            }
        }
    }

    // il metodo unfollow ci permette di smettere di seguire qualcuno
    public Risposta unfollow(String nome_utente, String da_non_seguire) {
        // se sconsideratamente sto cercando di smettere di seguire me stesso ritorno
        // errore
        if (da_non_seguire.equals(nome_utente)) {
            return new Risposta(CodiciRitorno.UNFOLLOW_FAILURE, "non puoi smettere di seguire te stesso !");
        }
        // acquisisco la lock sul database utenti
        synchronized (database_utenti) {
            // se l'utente che voglio smettere di seguire non e' registrato ritorno errore
            if (!database_utenti.containsKey(da_non_seguire)) {
                return new Risposta(CodiciRitorno.UNFOLLOW_FAILURE, "non esiste nessun " + da_non_seguire);
            }
            // altrimenti recupero il mio profilo
            Utente utente = database_utenti.get(nome_utente);
            // provo a rimuovere l'utente dai miei seguiti
            if (utente.rimuoviSeguito(da_non_seguire) == 0) {
                // se non lo seguivo, ritorno con fallimento
                return new Risposta(CodiciRitorno.UNFOLLOW_FAILURE, "non sei un follower di " + da_non_seguire);
            }
            // recupero il profilo dell'utente da smettere di seguire
            Utente perde_seguace = database_utenti.get(da_non_seguire);
            // provo a rimuovermi dalla lista dei suoi seguaci
            if (perde_seguace.rimuoviSeguace(nome_utente) == 0) {
                // se non ero un suo seguace, ritorno con fallimento
                return new Risposta(CodiciRitorno.UNFOLLOW_FAILURE,
                        da_non_seguire + "non era seguito da " + nome_utente);
            }
        }
        // se e' andato tutto bene ritorno con successo
        return new Risposta(CodiciRitorno.UNFOLLOW_SUCCESS, "hai smesso di seguire " + da_non_seguire);
    }

    // metodo createPost : ci permette di pubblicare un post su winsome
    public Risposta createPost(String nome_utente, String titolo, String contenuto) {
        // controllo che il titolo sia conforme alle regole, altrimenti ritorno con
        // fallimento
        if (titolo.length() > 20 || titolo.isBlank() || titolo.isEmpty()) {
            return new Risposta(CodiciRitorno.CREATEPOST_FAILURE, "inserire un titolo fino a 20 caratteri");
        }
        // controllo che il contenuto sia conforme alle regole, altrimenti ritorno con
        // fallimento
        if (contenuto.length() > 500 || contenuto.isBlank() || contenuto.isEmpty()) {
            return new Risposta(CodiciRitorno.CREATEPOST_FAILURE, "inserire un post fino a 500 caratteri");
        }
        // creo un nuovo post con gli argomenti passati, gli assegno un id dal contatore
        // globale, e un ciclo di premiazioni pari a 1 (= post nuovo)
        Post post = new Post(titolo, contenuto, nome_utente, id_post.getAndIncrement(), 1);

        // acquisisco la lock sul database dei blog
        synchronized (database_blog) {
            // se avevo postato gia' altri post aggiungo il nuovo post alla lista
            if (database_blog.containsKey(nome_utente)) {
                database_blog.get(nome_utente).add(post);
            }
            // se questo e' il mio primo post invece
            else {
                // creo una lista di post vuota
                ArrayList<Post> lista_post = new ArrayList<Post>();
                // ci aggiungo il mio nuovo post
                lista_post.add(post);
                // aggiungo in maniera thread-safe una nuova entry al database dei blog
                database_blog.put(nome_utente, lista_post);
            }
        }
        // se non ci sono stati problemi ritorno con successo
        return new Risposta(CodiciRitorno.CREATEPOST_SUCCESS,
                "Post pubblicato! \nid: " + post.getId() + "\ntitolo: " + titolo + "\ncontenuto: " + contenuto);
    }

    // il metodo showPost ci permette di visualizzare un post qualunque nel server
    // passando il suo id
    public Risposta showPost(int id) {
        // se ho passato l'id di un post non esistente ritorno errore
        if (id > id_post.get()) {
            return new Risposta(CodiciRitorno.SHOWPOST_FAILURE, "un post con questo id non puo' esistere");
        }
        // preparo la stringa di risposta
        StringBuilder risposta = new StringBuilder("\n");
        // acquisisco la lock sul database dei blog
        synchronized (database_blog) {
            // scorro tutti i blog
            for (ArrayList<Post> blog : database_blog.values()) {
                // scorro tutti i post di ogni blog
                for (int i = 0; i < blog.size(); i++) {
                    // se ho trovato il post in questione, lo recupero
                    if (blog.get(i).getId() == id) {
                        risposta.append("Titolo: " + blog.get(i).getTitolo() + "\n" + "Contenuto: "
                                + blog.get(i).getContenuto() + "\n"
                                + "Mi Piace: " + blog.get(i).numeroMiPiace()
                                + "\n" + "Non Mi Piace: " + blog.get(i).numeroNonMiPiace() + "\n" + "Commenti:\n");
                        // scorro tutti i commenti e li salvo
                        for (Commento commento : blog.get(i).listaCommenti()) {
                            risposta.append(" " + commento.getAutore() + ": " + commento.getCommento() + "\n");
                        }
                        // ritorno con successo la risposta
                        return new Risposta(CodiciRitorno.SHOWPOST_SUCCESS, risposta.toString());
                    }
                }
            }
            // se il post non esiste, ritorno con fallimento
            return new Risposta(CodiciRitorno.SHOWPOST_FAILURE, "post non esistente");
        }
    }

    // il metodo addComment ci permette di commentare un post di una persona che
    // seguiamo
    public Risposta addComment(String nome_utente, int post_id, String commento) {
        // controlli sul commento, se non e' valido ritorno con fallimento
        if (commento.length() > 100 || commento.isBlank() || commento.isEmpty()) {
            return new Risposta(CodiciRitorno.ADDCOMMENT_FAILURE, "inserire un commento di massimo 100 caratteri");
        }
        // se ho passato l'id di un post non esistente ritorno errore
        if (post_id > id_post.get()) {
            return new Risposta(CodiciRitorno.ADDCOMMENT_FAILURE, "un post con questo id non puo' esistere");
        }
        // acquisisco la lock sul database dei blog
        synchronized (database_blog) {
            // scorro tutti i blog
            for (ArrayList<Post> blog : database_blog.values()) {
                // scorro tutti i post nel blog
                for (int i = 0; i < blog.size(); i++) {
                    // se ho trovato il post in questione procedo
                    if (blog.get(i).getId() == post_id) {
                        // se sto cercando di commentare un mio post ritorno con fallimento
                        if (blog.get(i).getAutore().equals(nome_utente)) {
                            return new Risposta(CodiciRitorno.ADDCOMMENT_FAILURE, "non puoi commentare un tuo post");
                        }
                        // se sto cercando di commentare un post che non sia nel mio feed (di un utente
                        // che non seguo) ritorno con fallimento
                        // questa chiamata e' thread-safe perche' oltre ad usare
                        // ConcurrentHashMapsnessun altro thread potra' modificare la lista dei miei
                        // seguiti
                        if (!(database_utenti.get(nome_utente).getSeguiti().contains(blog.get(i).getAutore()))) {
                            return new Risposta(CodiciRitorno.ADDCOMMENT_FAILURE,
                                    "non puoi commentare un post che non sia nel tuo feed");
                        }
                        // se tutto va bene creo un nuovo commento
                        Commento nuovo_commento = new Commento(nome_utente, commento);
                        // aggiungo il nuovo commento
                        blog.get(i).commenta(nuovo_commento);
                        // ritorno con successo
                        return new Risposta(CodiciRitorno.ADDCOMMENT_SUCCESS, "commento aggiunto al post " + post_id);
                    }
                }
            }
        }
        // se il post non esisteva ritorno con fallimento
        return new Risposta(CodiciRitorno.ADDCOMMENT_FAILURE, "post non esistente");
    }

    // ratePost ci permette di valutare positivamente o (aut latino) negativamente
    // un post, una sola volta per post
    public Risposta ratePost(String nome_utente, int post_id, int votazione) {
        // se il voto non e' significativo ritorno con fallimento
        if (votazione != -1 && votazione != 1) {
            return new Risposta(CodiciRitorno.RATEPOST_FAILURE, "inserire un votazione positivo (+1) o negativo (-1)");
        }
        // se ho passato l'id di un post non esistente ritorno errore
        if (post_id > id_post.get()) {
            return new Risposta(CodiciRitorno.RATEPOST_FAILURE, "un post con questo id non puo' esistere");
        }
        // acquisisco la lock del database dei blog
        synchronized (database_blog) {
            // scorro tutti i blog
            for (ArrayList<Post> blog : database_blog.values()) {
                // scorro tutti i post dei blog
                for (int i = 0; i < blog.size(); i++) {
                    // se ho trovato il post in questione
                    if (blog.get(i).getId() == post_id) {
                        // e non sono io l'autore (altrimenti ritorno errore)
                        if (blog.get(i).getAutore().equals(nome_utente)) {
                            return new Risposta(CodiciRitorno.RATEPOST_FAILURE, "non puoi valutare un tuo post");
                        }
                        // posso quindi controllare in maniera thread-safe come spiegato a riga 408 se
                        // seguo l'autore del post
                        // se non lo seguo ritorno con fallimento
                        if (!(database_utenti.get(nome_utente).getSeguiti().contains(blog.get(i).getAutore()))) {
                            return new Risposta(CodiciRitorno.RATEPOST_FAILURE,
                                    "non puoi valutare un post che non sia nel tuo feed");
                        }
                        // se riesco ad aggiungere il non mi piace al post ritorno con successo
                        if (votazione == -1 && blog.get(i).nonMiPiace(nome_utente) == 1) {
                            return new Risposta(CodiciRitorno.RATEPOST_SUCCESS,
                                    "non ti piace il post numero " + post_id);
                        }
                        // se riesco ad aggiungere il mi piace al post ritorno con successo
                        else if (votazione == 1 && blog.get(i).miPiace(nome_utente) == 1) {
                            return new Risposta(CodiciRitorno.RATEPOST_SUCCESS,
                                    "non ti piace il post numero " + post_id);
                        }
                        // se arrivo qui significa che ho gia' valutato il post, ritorno quindi con
                        // fallimento
                        else {
                            return new Risposta(CodiciRitorno.RATEPOST_FAILURE, "non puoi valutare piu' volte un post");
                        }
                    }
                }
            }
        }
        // se non ho trovato il post, ritorno con fallimento
        return new Risposta(CodiciRitorno.RATEPOST_FAILURE, "post non esistente");
    }

    // deletePost ci permette di eliminare un post, se ne siamo gli autori
    public Risposta deletePost(String nome_utente, int post_id) {
        // se l'id del post non ha senso ritorno con fallimento
        if (post_id > id_post.get()) {
            return new Risposta(CodiciRitorno.DELETEPOST_FAILURE, "un post con questo id non puo' esistere");
        }
        // acquisisco la lock sui blog
        synchronized (database_blog) {
            // scorro i blog
            for (ArrayList<Post> blog : database_blog.values()) {
                // scorro i post
                for (int i = 0; i < blog.size(); i++) {
                    // se ho trovato il post in questione
                    if (blog.get(i).getId() == post_id) {
                        // se sto sconsideratamente cercando di eliminare un post che non e' mio ritorno
                        // con fallimento
                        if (!blog.get(i).getAutore().equals(nome_utente)) {
                            return new Risposta(CodiciRitorno.DELETEPOST_FAILURE,
                                    "non puoi eliminare un post di cui non sei l'autore");
                        }
                        // se il post e' mio allora lo rimuovo e ritorno con successo
                        else {
                            blog.remove(i);
                            //se ora il mio blog e' vuoto, cancello la mia entry dal database dei blog
                            if(blog.isEmpty()) {
                                database_blog.remove(blog.get(i).getAutore());
                            }
                            return new Risposta(CodiciRitorno.DELETEPOST_SUCCESS, "post eliminato");
                        }

                    }
                }
            }
        }
        // non ho trovato il post, ritorno con fallimento
        return new Risposta(CodiciRitorno.DELETEPOST_FAILURE, "post non esistente");
    }

    // rewinPost ci permette di ripubblicare un post di un'altra persona
    // in questa implementazione molto semplice ci limitiamo a copiare il post, e a
    // ripubblicarlo come se fossimo noi gli autori
    // tecnicamente siamo noi gli autori del post rewinnato, quindi non e' sbagliato
    // di per se' guadagnarci su
    // ovviamente si eredita il ciclo di premiazioni, ma non i like originali: non
    // esiste il rischio di lucro con l'abuso di questo metodo
    // sul modello di alcuni social ho deciso di permettere il rewin di propri post,
    // unicamente per una questione di visibilita'
    public Risposta rewinPost(String nome_utente, int post_id) {
        // se l'id del post passato non ha senso, ritorno con fallimento
        if (post_id > id_post.get()) {
            return new Risposta(CodiciRitorno.REWINPOST_FAILURE, "un post con questo id non puo' esistere");
        }
        // acquisisco la lock sul database dei blog
        synchronized (database_blog) {
            // scorro i blog
            for (ArrayList<Post> blog : database_blog.values()) {
                // scorro i post
                for (int i = 0; i < blog.size(); i++) {
                    // se ho trovato il post in questione
                    if (blog.get(i).getId() == post_id) {
                        // se sto cercando di repostare un post di un utente che non seguo (e che non
                        // sono io) ritornero' con fallimento
                        if (!(database_utenti.get(nome_utente).getSeguiti().contains(blog.get(i).getAutore()))
                                && !blog.get(i).getAutore().equals(nome_utente)) {
                            return new Risposta(CodiciRitorno.REWINPOST_FAILURE,
                                    "non puoi fare il rewin di un post che non sia nel tuo feed");
                        }
                        // se tutto va bene creo un post utilizzando il secondo costruttore, che
                        // permette di specificare l'autore originale del post
                        // che potrei essere anche io
                        else {
                            Post post = new Post(blog.get(i).getTitolo(), blog.get(i).getContenuto(), nome_utente,
                                    id_post.getAndIncrement(), blog.get(i).getItPremiazioni(), blog.get(i).getAutore());
                            // se non era il mio primo post lo aggiungo semplicemente alla lista
                            if (database_blog.containsKey(nome_utente)) {
                                database_blog.get(nome_utente).add(post);
                            }
                            // se era il mio primo post, creo la mia entry nel blog database e inserisco il
                            // post nella lista
                            else {
                                ArrayList<Post> lista_post = new ArrayList<Post>();
                                lista_post.add(post);
                                database_blog.put(nome_utente, lista_post);

                            }
                            // ritorno con successo
                            return new Risposta(CodiciRitorno.REWINPOST_SUCCESS, "rewin del post " + post_id);
                        }
                    }
                }
            }
        }
        // se il post non esiste, ritorno con fallimento
        return new Risposta(CodiciRitorno.REWINPOST_FAILURE, "post non esistente");
    }

    // viewblog ci permette di visualizzare tutti i post che abbiamo pubblicato
    public Risposta viewBlog(String nome_utente) {
        // preparo risposta
        StringBuilder risposta = new StringBuilder("\nId \t| Autore \t| Titolo\n");
        // lock sui blog
        synchronized (database_blog) {
            // se non esiste la mia entry, non ho mai postato - ritorno con fallimento
            if (database_blog.get(nome_utente) == null || database_blog.get(nome_utente).isEmpty()) {
                return new Risposta(CodiciRitorno.VIEWBLOG_FAILURE, "il tuo blog e' vuoto");
            }
            // altrimenti scorro tutti i miei post e li salvo in risposta
            for (Post post : database_blog.get(nome_utente)) {
                // se non e' un rewin, non segno l'autore originale
                if (post.getRewinned() == null) {
                    risposta.append(post.getId() + "\t|" +
                            post.getAutore() + "\t|"
                            + post.getTitolo() + "\n");
                }
                // se e' un rewin, mi segno l'autore originale
                else {
                    risposta.append(post.getId() + "\t|" +
                            post.getAutore() + " (rewinned from: " + post.getRewinned() + ")\t|"
                            + post.getTitolo() + "\n");
                }
            }
            // ritorno con successo il mio blog
            return new Risposta(CodiciRitorno.VIEWBLOG_SUCCESS, risposta.toString());
        }

    }

    // getWallet ci mostra il nostro portafoglio: il risultato di questa
    // interrogazione e' un caso di
    // "eventual consistency", significa che si dovra' aspettare il ciclo periodico
    // di premiazioni per avere un risultato consistente
    public Risposta getWallet(String nome_utente) {
        // lock sui portafogli
        synchronized (database_portafogli) {
            // se ho un portafogli
            if (database_portafogli.containsKey(nome_utente)) {
                // ritorno con successo lo stato del mio portafoglio
                return new Risposta(CodiciRitorno.GETWALLET_SUCCESS,
                        "possiedi " + database_portafogli.get(nome_utente).getTotaleWincoins()
                                + " wincoins !\nStorico delle transazioni: \n"
                                + database_portafogli.get(nome_utente).getTransazioni().toString());
            }
            // altrimenti, se non ho un portafogli, ritorno con fallimento
            return new Risposta(CodiciRitorno.GETWALLET_FAILURE, "che strano, non hai un portafogli...");
        }
    }

    // questo metodo ci permette, con un cambio casuale generato tramite una
    // richiesta http a un sito esterno, di convertire i nostri wincoins in bitcoins
    public Risposta getWalletInBitcoin(String nome_utente) {
        // valore del cambio casuale
        double cambio;
        // richiesta http
        HttpURLConnection http_request = null;

        try {
            // preparo la url con la query al sito random.org: ritornera' un numero tra uno
            // e dieci
            URL url = new URL(
                    "https://www.random.org/integers/?num=1&min=1&max=10&col=1&base=10&format=plain&rnd=new");
            // apro connessione
            http_request = (HttpURLConnection) url.openConnection();
            // se va tutto bene
            if (http_request.getResponseCode() == 200) {
                // ricavo la risposta
                try (BufferedReader buffer_risposta = new BufferedReader(
                        new InputStreamReader(http_request.getInputStream()))) {
                    // ricavo il cambio parsando un double
                    cambio = Double.parseDouble(buffer_risposta.readLine());
                }
            }
            // se qualcosa e' andato storto
            else {
                // ritorno con fallimento
                return new Risposta(CodiciRitorno.GETWALLETINBITCOIN_FAILURE,
                        "richiesta http non andata a buon fine");
            }

        } catch (IOException e) {
            // se non riesco a connettermi
            return new Risposta(CodiciRitorno.GETWALLETINBITCOIN_FAILURE, "impossibile connettersi al sito random.org");
        }
        // alla fine, mi disconnettero'
        finally {
            if (http_request != null)
                http_request.disconnect();
        }
        // lock sui portafogli
        synchronized (database_portafogli) {
            // se ho un portafogli, ritorno il valore in bitcoin dopo aver arrotondato alla
            // seconda cifra decimale
            if (database_portafogli.containsKey(nome_utente)) {
                BigDecimal bd = BigDecimal.valueOf((database_portafogli.get(nome_utente).getTotaleWincoins() / cambio));
                bd = bd.setScale(3, RoundingMode.HALF_UP);
                return new Risposta(CodiciRitorno.GETWALLETINBITCOIN_SUCCESS,
                        "possiedi " + bd.doubleValue() + " bitcoins !\nStorico transazioni:\n"
                                + database_portafogli.get(nome_utente).getTransazioni().toString());
            }

        }
        // se non ho un portafoglio ritorno con fallimento
        return new Risposta(CodiciRitorno.GETWALLETINBITCOIN_FAILURE, "che strano, non hai un portafogli...");

    }

    // chiamata RMI, permette al client di ritirare la sua lista di followers
    public ArrayList<String> getFollowers(String nome_utente) {
        // copia della lista seguaci
        ArrayList<String> seguaci = new ArrayList<String>();
        // lock sugli utenti
        synchronized (database_utenti) {
            // copio i miei seguaci
            seguaci.addAll(database_utenti.get(nome_utente).getSeguaci());
        }
        // ritorno lista seguaci
        return seguaci;
    }

    // metodo per salvare lo stato del server, viene invocato periodicamente dal
    // thread di backup
    // salva su file json separati le principali strutture dati di WinsomeServer,
    // tranne ovviamente utenti_loggati
    public void salva_stato_server() {
        // attivo indentazione pretty printing dei file json
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            // lock su id_post e scrittura su file json
            synchronized (id_post) {
                mapper.writeValue(Paths.get("json/JSONidpost.json").toFile(), this.id_post);
            }
            // lock su utenti e scrittura su file json
            synchronized (database_utenti) {
                mapper.writeValue(Paths.get("json/JSONutenti.json").toFile(), this.database_utenti);
            }
            // lock su blog e scrittura su file json
            synchronized (database_blog) {
                mapper.writeValue(Paths.get("json/JSONblog.json").toFile(), this.database_blog);
            }
            // lock su portafogli e scrittura su file json
            synchronized (database_portafogli) {
                mapper.writeValue(Paths.get("json/JSONportafogli.json").toFile(), this.database_portafogli);
            }

        } catch (IOException e) {
            // errore nella scrittura dei json
            System.out.println("errore nella fase di backup");
        }
    }

    // metodo per aggiornare i portafogli degli utenti meritevoli, viene invocato
    // periodicamente dal thread di pagamento multicast
    public void aggiorna_portafogli() {
        // lock sui blog
        synchronized (database_blog) {
            // scorro per ogni utente
            for (String utente : database_blog.keySet()) {
                // se l'utente ha pubblicato post
                if (database_blog.get(utente) != null && !database_blog.get(utente).isEmpty()) {
                    // scorro tutti i suoi post
                    for (Post post : database_blog.get(utente)) {
                        // creo una lista di curatori, i commentatori e i valutatori
                        ArrayList<String> utenti_curatori = new ArrayList<String>();

                        // calcolo prima la sommatoria della parte relativa ai commenti
                        int sommatoria_commenti = 0;
                        // per ogni commento ancora da valutare
                        for (Commento commento : post.getCommentiDaPremiare()) {
                            // inserisco ogni autore di commenti una volta sola in questa lista di curatori
                            if (!utenti_curatori.contains(commento.getAutore())) {
                                utenti_curatori.add(commento.getAutore());
                                // per contrastare lo spam dei commenti applico questo trucco tenendo conto del
                                // numero dei miei commenti
                                sommatoria_commenti += 2
                                        / (1 + Math.exp(-(post.numeroCommentiDi(commento.getAutore()) - 1)));
                            }
                        }
                        // metto via questi commenti, non li considerero' piu' per premiarti
                        post.getCommentiPremiati().addAll(post.getCommentiDaPremiare());
                        post.setCommentiDaPremiare(new ArrayList<Commento>());
                        // guadagno totale per la parte dei commenti secondo l'equazione in consegna:
                        double guadagno_commenti = Math.log(sommatoria_commenti + 1);

                        // poi calcolo la sommatoria per le valutazioni: considerero' il massimo tra 0 e
                        // questo numero possibilmente negativo
                        int sommatoria_valutazioni = post.getLikesDaPremiare().size()
                                - post.getDislikesDaPremiare().size();
                        // se non sono gia' nella lista dei curatori e ho messo mi piace, mi ci metto
                        for (String curatore : post.getLikesDaPremiare()) {
                            if (!utenti_curatori.contains(curatore)) {
                                utenti_curatori.add(curatore);
                            }
                        }
                        // metto via questi mi piace e questi non mi piace, non li considerero' piu' per
                        // premiarti
                        post.getLikesPremiati().addAll(post.getLikesDaPremiare());
                        post.setLikesDaPremiare(new ArrayList<String>());
                        post.getDislikesPremiati().addAll(post.getDislikesDaPremiare());
                        post.setDislikesDaPremiare(new ArrayList<String>());
                        // guadagno totale per la parte dei mi piace secondo l'equazione:
                        double guadagno_mi_piace = Math.log(Math.max(sommatoria_valutazioni, 0) + 1);

                        // guadagno totale:
                        // recupero l'iterazione della premiazione
                        int ciclo_premiazione = post.getItPremiazioni();
                        // concludo il calcolo dell'equazioni
                        double guadagno_totale = (guadagno_mi_piace + guadagno_commenti) / ciclo_premiazione;
                        // aggiorno il ciclo di iterazioni del post
                        post.setItPremiazioni(ciclo_premiazione + 1);
                        // premio ogni curatore in base alla percentuale autore ricavata dal file di
                        // configurazione
                        for (String curatore : utenti_curatori) {
                            database_portafogli.get(curatore).aggiungiWincoin(
                                    guadagno_totale * (1 - configurazione.getPercentualeAutore() / 100)
                                            / utenti_curatori.size());
                        }
                        // premio l'autore del post in base alla percentuale autore ricavata dal file di
                        // configurazione
                        database_portafogli.get(utente)
                                .aggiungiWincoin(guadagno_totale * (configurazione.getPercentualeAutore() / 100));
                    }
                }
            }
        }
    }
}
