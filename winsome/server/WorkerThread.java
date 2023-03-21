import java.nio.channels.SelectionKey;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

//classe thread worker che effettua le chiamate a winsome
public class WorkerThread implements Runnable {
    // chiave associata al socket di un certo client
    SelectionKey chiave;
    // riferimento al server winsome
    WinsomeServer winsome;
    // oggetto RMI server
    RmiServer rmi_server;

    // costruttore
    public WorkerThread(SelectionKey chiave, WinsomeServer winsome, RmiServer rmi_server) {

        this.chiave = chiave;
        this.winsome = winsome;
        this.rmi_server = rmi_server;
    }

    @Override
    public void run() {
        // recupero le informazioni dall'attachment della chiave
        ClientEnd client_end = (ClientEnd) chiave.attachment();
        // recupero la richiesta
        String richiesta = client_end.richiesta;
        // stringa per il tipo di operazione
        String op = null;

        try {
            // divido la richiesta in token
            StringTokenizer tokenizer = new StringTokenizer(richiesta);
            // ricavo l'operazione
            op = tokenizer.nextToken();
            // se sto provando a fare qualche operazione che non sia il login, non essendo
            // loggato, ritorno con fallimento
            if (!client_end.accesso && !op.equals("login")) {
                client_end.risposta = new Risposta(CodiciRitorno.BAD_REQUEST, "prima di qualsiasi altra operazione devi eseguire il login !");

            }
            // altrimenti procedo
            else {
                // switch sul tipo di operazione
                switch (op) {

                    case "login":
                        // se sono gia' connesso ritorno con fallimento
                        if (client_end.accesso) {
                            client_end.risposta = new Risposta(CodiciRitorno.LOGIN_FAILURE, "errore, sei gia' loggato !");
                            break;
                        }
                        // ricavo nome utente
                        String nome_utente = tokenizer.nextToken();
                        // chiamo winsome
                        client_end.risposta = winsome.login(nome_utente, tokenizer.nextToken());
                        // se la chiamata ha avuto successo modifico le variabili di stato del client
                        if (client_end.risposta.getRisultato() == CodiciRitorno.LOGIN_SUCCESS) {
                            client_end.accesso = true;
                            client_end.nome_utente = nome_utente;
                        }
                        break;
                    case "logout":
                        // chiamo winsome
                        client_end.risposta = winsome.logout(client_end.nome_utente);
                        // in tutti i casi modifico le variabili di stato del client
                        client_end.accesso = false;
                        client_end.nome_utente = null;
                        break;
                    case "list":
                        // switch ulteriore: ci sono diverse "list"
                        switch (tokenizer.nextToken()) {
                            case "users":
                                // chiamo winsome
                                client_end.risposta = winsome.listUsers(client_end.nome_utente);

                                break;
                            case "following":
                                // chiamo winsome
                                client_end.risposta = winsome.listFollowing(client_end.nome_utente);

                                break;
                            default:
                                // chiamo winsome
                                client_end.risposta = new Risposta(CodiciRitorno.BAD_REQUEST,
                                        "errore, listing case non valido !");
                                break;
                        }
                        break;

                    case "follow":
                        // recupero l'utente da seguire
                        String da_seguire = tokenizer.nextToken();
                        // chiamo winsome
                        client_end.risposta = winsome.follow(client_end.nome_utente, da_seguire);
                        // se l'operazione ha avuto successo, notifico il client con una callback
                        if (client_end.risposta.getRisultato() == CodiciRitorno.FOLLOW_SUCCESS) {
                            rmi_server.followCallback(da_seguire, client_end.nome_utente);
                        }
                        break;

                    case "unfollow":
                        // recupero l'utente da smettere di seguire
                        String da_non_seguire = tokenizer.nextToken();
                        // chiamo winsome
                        client_end.risposta = winsome.unfollow(client_end.nome_utente, da_non_seguire);
                        // se l'operazione ha avuto successo, notifico il client con una callback
                        if (client_end.risposta.getRisultato() == CodiciRitorno.UNFOLLOW_SUCCESS) {
                            rmi_server.unfollowCallback(da_non_seguire, client_end.nome_utente);
                        }

                        break;

                    case "post":
                        // suddivido la richiesta in segmenti, separando per virgolette
                        String[] seg = richiesta.trim().split("\"");
                        // se e' andato tutto bene dovrei avere 4 segmenti, mi interessano il secondo e
                        // il quarto
                        // che sono il titolo e il contenuto del post
                        if (seg.length == 4) {
                            // chiamo winsome
                            client_end.risposta = winsome.createPost(client_end.nome_utente, seg[1], seg[3]);
                        } else {
                            // altrimenti ritorno con fallimento
                            client_end.risposta = new Risposta(CodiciRitorno.CREATEPOST_FAILURE,
                                    "errore, strutturare la richiesta con \"titolo\" e \"post\" tra virgolette !");
                        }
                        break;
                    case "blog":
                        // chiamo winsome
                        client_end.risposta = winsome.viewBlog(client_end.nome_utente);
                        break;
                    case "show":
                        // ulteriore switch su show: ci sono diversi casi
                        switch (tokenizer.nextToken()) {
                            case "feed":
                                // chiamo winsome
                                client_end.risposta = winsome.showFeed(client_end.nome_utente);
                                break;
                            case "post":
                                // chiamo winsome
                                client_end.risposta = winsome.showPost(Integer.parseInt(tokenizer.nextToken()));
                                break;
                            default:
                                // ritorno con fallimento, bad request
                                client_end.risposta = new Risposta(CodiciRitorno.BAD_REQUEST,
                                        "errore, showing case non valido !");
                                break;
                        }
                        break;
                    case "rewin":
                        // chiamo winsome
                        client_end.risposta = winsome.rewinPost(client_end.nome_utente,
                                Integer.parseInt(tokenizer.nextToken()));

                        break;

                    case "delete":
                        // chiamo winsome
                        client_end.risposta = winsome.deletePost(client_end.nome_utente,
                                Integer.parseInt(tokenizer.nextToken()));

                        break;

                    case "rate":
                        // chiamo winsome
                        client_end.risposta = winsome.ratePost(client_end.nome_utente,
                                Integer.parseInt(tokenizer.nextToken()),
                                Integer.parseInt(tokenizer.nextToken()));

                        break;

                    case "wallet":
                        // potrei aver chiamato "wallet btc"
                        if (tokenizer.hasMoreTokens()) {
                            // se si, chiamo winsome
                            if (tokenizer.nextToken().equals("btc")) {
                                client_end.risposta = winsome.getWalletInBitcoin(client_end.nome_utente);
                                break;
                            } else {
                                // altrimenti bad request
                                client_end.risposta = new Risposta(CodiciRitorno.BAD_REQUEST, "errore, wallet case non valido !");
                                break;
                            }

                        } else {
                            // chiamo winsome
                            client_end.risposta = winsome.getWallet(client_end.nome_utente);
                        }
                        break;

                    case "commento":
                        // ricavo l'id del post
                        int post_id = Integer.parseInt(tokenizer.nextToken());
                        // ricavo il commento
                        String commento = "";
                        while (tokenizer.hasMoreTokens()) {
                            commento += " " + tokenizer.nextToken();
                        }
                        // chiamo winsome
                        client_end.risposta = winsome.addComment(client_end.nome_utente, post_id, commento);

                        break;

                    default:
                        // ritorno bad request
                        client_end.risposta = new Risposta(CodiciRitorno.BAD_REQUEST,
                                "errore, comando non supportato. consultare sezione HELP");
                        break;
                }
            }

            // scatenata dal parser, se non ci sono ulteriori elementi da parsare
        } catch (NoSuchElementException e) {
            // ho bisogno di sapere se l'operazione era un login per poterla gestire lato
            // client
            if (op.equals("login")) {
                client_end.risposta = new Risposta(CodiciRitorno.LOGIN_FAILURE,
                        "errore nella formulazione della login. consultare sezione HELP");
            }
            // ritorno comunque errore, bad request
            else {
                client_end.risposta = new Risposta(CodiciRitorno.BAD_REQUEST,
                        "errore nella formulazione della richiesta. consultare sezione HELP");
            }
        }
        // azzero la richiesta
        client_end.richiesta = null;

        try {
            // segnalo che il client e' pronto in lettura, con attachment il client_end con
            // annesso lo status
            chiave.attach(client_end);
            chiave.interestOps(SelectionKey.OP_WRITE);
            chiave.selector().wakeup();

        } catch (Exception e) {
            // errore fatale
            System.out.println("errore fatale sul selettore !");
            System.exit(-1);
        }

    }
}