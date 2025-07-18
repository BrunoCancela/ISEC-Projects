package pt.isec.pd.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DatabaseTransferInterface extends Remote {
    /**
     * Retrieves the entire database as a byte array.
     *
     * @return the database in a byte array format.
     * @throws RemoteException if a remote communication error occurs.
     */
    byte[] getDatabase() throws RemoteException;
}