import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MatrixService extends Remote {

    int multiply(int[] row, int[] column) throws RemoteException;

    boolean heartbeat() throws RemoteException;
}