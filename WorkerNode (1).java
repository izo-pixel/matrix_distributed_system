import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.Arrays;

public class WorkerNode extends UnicastRemoteObject implements MatrixService {

    private final String workerName;

    protected WorkerNode(String name) throws RemoteException {
        super();
        this.workerName = name;
    }

    @Override
    public synchronized int multiply(int[] row, int[] column) throws RemoteException {

        if (row.length != column.length) {
            throw new RemoteException("Matrix size mismatch");
        }

        System.out.println(workerName + " received row " + Arrays.toString(row));
        System.out.println(workerName + " received column " + Arrays.toString(column));

        int sum = 0;

        for (int i = 0; i < row.length; i++) {

            int product = row[i] * column[i];
            sum += product;

            System.out.println(workerName + " computing " + row[i] + " * " + column[i]);
        }

        System.out.println(workerName + " result = " + sum);
        return sum;
    }

    @Override
    public boolean heartbeat() throws RemoteException {
        return true;
    }
}