import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MasterNode {
    
    private static final AtomicInteger taskCounter = new AtomicInteger(0);
    private static Map<String, WorkerStats> workerStatsMap = new ConcurrentHashMap<>();
    private static List<MatrixService> healthyWorkers = new CopyOnWriteArrayList<>();
    
    static class WorkerStats {
        int taskId;
        long lastUsed;
        int successCount;
        int failureCount;
        
        WorkerStats() {
            this.taskId = 0;
            this.lastUsed = System.currentTimeMillis();
            this.successCount = 0;
            this.failureCount = 0;
        }
    }

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        List<MatrixService> workers = new ArrayList<>();

        try {

            String[] workerIPs = {
                    "192.168.43.186",
                    "192.168.43.243"
            };

            int port = 1099;

            for (String ip : workerIPs) {

                try {

                    Registry registry = LocateRegistry.getRegistry(ip, port);
                    String[] names = registry.list();

                    for (String name : names) {

                        MatrixService worker =
                                (MatrixService) registry.lookup(name);

                        if (worker.heartbeat()) {

                            workers.add(worker);

                            System.out.println("Connected to worker " + name + " on " + ip);
                        }
                    }

                } catch (Exception e) {

                    System.out.println("Worker unavailable at " + ip);
                }
            }

            System.out.println("Total workers available: " + workers.size());
            
            if (workers.size() > 0) {
                System.out.println("\nStarting distributed matrix multiplication with load balancing...");
            } else {
                System.out.println("\nNo workers available. Running in local mode only.");
            }

            int[][] A = new int[2][2];
            int[][] B = new int[2][2];
            int[][] result = new int[2][2];

            System.out.println("Enter Matrix A (2x2)");

            for (int i = 0; i < 2; i++)
                for (int j = 0; j < 2; j++)
                    A[i][j] = sc.nextInt();

            System.out.println("Enter Matrix B (2x2)");

            for (int i = 0; i < 2; i++)
                for (int j = 0; j < 2; j++)
                    B[i][j] = sc.nextInt();

            int[] col1 = {B[0][0], B[1][0]};
            int[] col2 = {B[0][1], B[1][1]};

            ExecutorService executor = Executors.newFixedThreadPool(Math.min(4, workers.size() * 2));
            
            System.out.println("Distributing matrix multiplication tasks across " + workers.size() + " workers");
            
            List<Future<Integer>> futures = new ArrayList<>();
            
            futures.add(executor.submit(() -> computeWithLoadBalancing(workers, A[0], col1, "r00")));
            futures.add(executor.submit(() -> computeWithLoadBalancing(workers, A[0], col2, "r01")));
            futures.add(executor.submit(() -> computeWithLoadBalancing(workers, A[1], col1, "r10")));
            futures.add(executor.submit(() -> computeWithLoadBalancing(workers, A[1], col2, "r11")));
            
            result[0][0] = futures.get(0).get();
            result[0][1] = futures.get(1).get();
            result[1][0] = futures.get(2).get();
            result[1][1] = futures.get(3).get();

            executor.shutdown();

            System.out.println("\nFinal Result Matrix");

            for (int i = 0; i < 2; i++) {

                for (int j = 0; j < 2; j++) {

                    System.out.print(result[i][j] + " ");
                }

                System.out.println();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        sc.close();
    }

    private static int computeWithLoadBalancing(List<MatrixService> workers, int[] row, int[] col, String taskId) {
        if (workers.isEmpty()) {
            System.out.println("[" + taskId + "] No workers available. Master computing task locally.");
            return localMultiply(row, col);
        }
        
        MatrixService selectedWorker = selectWorker(workers, taskId);
        
        try {
            long startTime = System.currentTimeMillis();
            System.out.println("[" + taskId + "] Sending task to worker (Load balanced)");
            
            int result = selectedWorker.multiply(row, col);
            
            long endTime = System.currentTimeMillis();
            System.out.println("[" + taskId + "] Worker completed task in " + (endTime - startTime) + "ms");
            
            updateWorkerStats(selectedWorker, true);
            return result;
            
        } catch (Exception e) {
            System.out.println("[" + taskId + "] Worker failed: " + e.getMessage() + ". Trying failover...");
            updateWorkerStats(selectedWorker, false);
            
            return tryFailoverWorkers(workers, selectedWorker, row, col, taskId);
        }
    }
    
    private static MatrixService selectWorker(List<MatrixService> workers, String taskId) {
        if (workers.size() == 1) {
            return workers.get(0);
        }
        
        MatrixService bestWorker = null;
        int minTasks = Integer.MAX_VALUE;
        
        for (MatrixService worker : workers) {
            String workerKey = worker.toString();
            WorkerStats stats = workerStatsMap.getOrDefault(workerKey, new WorkerStats());
            
            if (stats.taskId < minTasks) {
                minTasks = stats.taskId;
                bestWorker = worker;
            }
        }
        
        if (bestWorker != null) {
            String workerKey = bestWorker.toString();
            WorkerStats stats = workerStatsMap.getOrDefault(workerKey, new WorkerStats());
            stats.taskId++;
            stats.lastUsed = System.currentTimeMillis();
            workerStatsMap.put(workerKey, stats);
        }
        
        return bestWorker != null ? bestWorker : workers.get(0);
    }
    
    private static void updateWorkerStats(MatrixService worker, boolean success) {
        String workerKey = worker.toString();
        WorkerStats stats = workerStatsMap.getOrDefault(workerKey, new WorkerStats());
        
        if (success) {
            stats.successCount++;
            stats.taskId--;
        } else {
            stats.failureCount++;
        }
        
        workerStatsMap.put(workerKey, stats);
    }
    
    private static int tryFailoverWorkers(List<MatrixService> workers, MatrixService failedWorker, 
                                        int[] row, int[] col, String taskId) {
        for (MatrixService worker : workers) {
            if (worker == failedWorker) continue;
            
            try {
                System.out.println("[" + taskId + "] Trying failover worker");
                int result = worker.multiply(row, col);
                System.out.println("[" + taskId + "] Failover worker completed task successfully");
                updateWorkerStats(worker, true);
                return result;
            } catch (Exception e) {
                System.out.println("[" + taskId + "] Failover worker also failed: " + e.getMessage());
                updateWorkerStats(worker, false);
            }
        }
        
        System.out.println("[" + taskId + "] All workers failed. Master computing task locally.");
        return localMultiply(row, col);
    }

    private static int localMultiply(int[] row, int[] col) {
        int sum = 0;
        for (int i = 0; i < row.length; i++) {
            sum += row[i] * col[i];
        }
        return sum;
    }
}