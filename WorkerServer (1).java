import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class WorkerServer {

    public static void main(String[] args) {

        try {

            if (args.length < 2) {
                System.out.println("Usage: java WorkerServer <WorkerName> <Port>");
                return;
            }

            String workerName = args[0];
            int port = Integer.parseInt(args[1]);

            Registry registry = LocateRegistry.createRegistry(port);

            WorkerNode worker = new WorkerNode(workerName);

            registry.rebind(workerName, worker);

            System.out.println(workerName + " started on port " + port);
            System.out.println(workerName + " waiting for tasks from master");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}