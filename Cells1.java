import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

public class Cells1 {
    static int N, K;
    static double p;
    static volatile boolean running = true;
    static final int STEP_DELAY_MS = 1;

    static int[] cells;
    static Object[] cellLocks;

    static boolean useArrayLock;
    static final LongAdder stepCounter = new LongAdder();

    static class Atom extends Thread {
        int pos = 0;

        @Override
        public void run() {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            while (running) {
                double m = rnd.nextDouble();
                int next = (m > p) ? pos + 1 : pos - 1;

                if (next < 0 || next >= N) {
                } else {
                    if (useArrayLock) {
                        synchronized (cells) {
                            cells[pos]--;
                            cells[next]++;
                            pos = next;
                        }
                    } else {
                        int a = Math.min(pos, next);
                        int b = Math.max(pos, next);
                        synchronized (cellLocks[a]) {
                            synchronized (cellLocks[b]) {
                                cells[pos]--;
                                cells[next]++;
                                pos = next;
                            }
                        }
                    }
                }

                stepCounter.increment();
                try {
                    Thread.sleep(STEP_DELAY_MS);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public static int getcell(int i) {
        if (useArrayLock) {
            synchronized (cells) {
                return cells[i];
            }
        } else {
            synchronized (cellLocks[i]) {
                return cells[i];
            }
        }
    }

    public static int getCell(int i) {
        return getcell(i);
    }

    static void printSnapshot(int sec) {
        int[] snap = new int[N];
        int sum = 0;

        if (useArrayLock) {
            synchronized (cells) {
                for (int i = 0; i < N; i++) {
                    snap[i] = cells[i];
                    sum += snap[i];
                }
            }
        } else {
            for (int i = 0; i < N; i++) {
                synchronized (cellLocks[i]) {
                    snap[i] = cells[i];
                }
                sum += snap[i];
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[").append(useArrayLock ? "lock=array" : "lock=cell").append("] ");
        sb.append("t=").append(sec).append("s: [");
        for (int i = 0; i < N; i++) {
            if (i > 0)
                sb.append(' ');
            sb.append(snap[i]);
        }
        sb.append("]  total=").append(sum);
        long ops = stepCounter.sumThenReset();
        sb.append("  steps/s=").append(ops);

        System.out.println(sb);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: java -Dlock={array|cell} Cells1 N K p");
            return;
        }
        N = Integer.parseInt(args[0]);
        K = Integer.parseInt(args[1]);
        p = Double.parseDouble(args[2]);
        if (N <= 0 || K < 0 || p < 0.0 || p > 1.0) {
            System.err.println("Invalid params: N>0, K>=0, 0<=p<=1");
            return;
        }

        String mode = System.getProperty("lock", "array").toLowerCase();
        useArrayLock = mode.equals("array");

        cells = new int[N];
        cells[0] = K;

        if (!useArrayLock) {
            cellLocks = new Object[N];
            for (int i = 0; i < N; i++)
                cellLocks[i] = new Object();
        }

        List<Atom> atoms = new ArrayList<>(K);
        for (int i = 0; i < K; i++) {
            Atom t = new Atom();
            atoms.add(t);
            t.start();
        }

        printSnapshot(0);
        for (int s = 1; s <= 60; s++) {
            Thread.sleep(1000);
            printSnapshot(s);
        }

        running = false;
        for (Thread t : atoms)
            t.join();

        int total = 0;
        if (useArrayLock) {
            synchronized (cells) {
                for (int i = 0; i < N; i++)
                    total += cells[i];
            }
        } else {
            for (int i = 0; i < N; i++) {
                synchronized (cellLocks[i]) {
                    total += cells[i];
                }
            }
        }
        System.out.println("FINAL total=" + total + "  (expected K=" + K + ")");
    }
}

