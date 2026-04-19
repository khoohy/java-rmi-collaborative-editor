package distributed.editor;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface DocumentService extends Remote {
    List<String> getDocumentLines() throws RemoteException;
    boolean requestLock(int lineIndex, String clientId) throws RemoteException;
    void updateLine(int lineIndex, String newText, String clientId) throws RemoteException;
    void releaseLock(int lineIndex, String clientId) throws RemoteException;
    void releaseAllLocks(String clientId) throws RemoteException;
    void addLine(String clientId) throws RemoteException;
    boolean deleteLine(int lineIndex) throws RemoteException;
    long getLastModified() throws RemoteException;
    Map<Integer, String> getActiveLocks() throws RemoteException; 
}