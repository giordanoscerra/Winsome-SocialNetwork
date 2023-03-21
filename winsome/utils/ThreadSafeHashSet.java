import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//classe che rende thread-safe l'hash set della libreria utils di java
public class ThreadSafeHashSet <T>{
    //hashset
    final private HashSet<T> map;
    //lock lettura e scritture per l'accesso mutualmente esclusivo
    final private Lock readLock;
    final private Lock writeLock;

    //costruttore
    public ThreadSafeHashSet (){
        map = new HashSet<>();
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        readLock = rwLock.readLock();
        writeLock = rwLock.writeLock();
    }

    //aggiunge un elemento al set, lockando con writelock
    public boolean add (T elem){
        try{
            writeLock.lock();
            return map.add(elem);
        }finally {
            writeLock.unlock();
        }
    }
    //elimina un elemento dal set, lockando con writelock
    public boolean remove (T elem){
        try{
            writeLock.lock();
            return map.remove(elem);
        }finally {
            writeLock.unlock();
        }
    }

    //verifica se il set contiene l'elemento, lockando con una readlock
    public boolean contains (T elem){
        try{
            readLock.lock();
            return map.contains(elem);
        }finally {
            readLock.unlock();
        }
    }
    //ritorna una copia del set, lockando con readlock
    public HashSet<T> getHashSet() {
        try{
            this.readLock.lock();
            return new HashSet<>(this.map);
        }finally {
            this.readLock.unlock();
        }
    }
}
