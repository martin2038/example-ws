import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * testAtomicLong: 2455, [267, 165, 271, 264, 179, 259, 264, 262, 261, 263]
 * // about 10 times than AtomicLong , and Striped64 no false sharing
 * testLongAdder: 266, [25, 30, 27, 26, 25, 26, 25, 25, 31, 26]
 * // falseSharing ... so LongAdder more quick than just normal long++. 肯定处于同一个cache line里了。
 * testLongNotExact: 425, [42, 43, 43, 43, 42, 44, 42, 42, 42, 42] Wrong ! 1946292
 * //简单的多线程加自己的模块，用更多内存，数量级接近LongAdder了
 * testMyLongAdder: 380, [37, 32, 34, 36, 39, 39, 39, 45, 41, 38]
 * // ReentrantLock  1.5 times of AtomicLong
 * testLongLock: 3749, [360, 343, 396, 340, 396, 392, 336, 395, 401, 390]
 * testLongSync: 13764, [1368, 1389, 1392, 1430, 1443, 1382, 1396, 1311, 1256, 1397]
 *
 *
 * 1）x、y位于同一行，2）两个CPU会频繁的访问这两个数据，如果这两个条件其中一个不成立，那就不会产生问题。
 * 更多关于伪共享的概念参考伪共享(False Sharing)和(false sharing(wiki)。
 *
 * 牛逼的 Doug Lea
 */
public class TestAtomic {

    private static final int TASK_NUM           = 1000;
    private static final int INCREMENT_PER_TASK = 10000;
    private static final int REPEAT             = 10;

    private static long normal, syncl, lockl = 0;

    public static void main(String[] args) {
        runAll();
        runAll();
        runAll();
        runAll();
        runAll();
        System.out.println("hot code ok..");
        runAll();
    }

    public static void runAll() {
        repeatWithStatics(REPEAT, () -> testAtomicLong());
        repeatWithStatics(REPEAT, () -> testLongAdder());
        repeatWithStatics(REPEAT, () -> testLongNotExact());
        repeatWithStatics(REPEAT, () -> testMyLongAdder());
        System.out.println("testLongNotExact : " + normal);
        repeatWithStatics(REPEAT, () -> testLongLock());
        System.out.println("testLongLock : " + lockl);
        repeatWithStatics(REPEAT, () -> testLongSync());
        System.out.println("testLongSync : " + syncl);
        System.out.println("----------------------------------------");
        System.out.println("----------------------------------------");
    }

    public static void testAtomicLong() {
        AtomicLong al = new AtomicLong(0);
        execute(TASK_NUM, () -> repeat(INCREMENT_PER_TASK, () -> al.incrementAndGet()));
    }

    public static void testLongNotExact() {
        normal = 0;
        execute(TASK_NUM, () -> repeat(INCREMENT_PER_TASK, () -> normal++));

    }

    public static void testLongLock() {
        lockl = 0;
        var lock = new ReentrantLock();
        execute(TASK_NUM, () -> repeat(INCREMENT_PER_TASK, () -> {
            try {
                lock.lock();
                lockl++;
            } finally {
                lock.unlock();
            }
        }));

    }

    public static void testLongSync() {
        syncl = 0;
        Object lock = new Object();
        execute(TASK_NUM, () -> repeat(INCREMENT_PER_TASK, () -> {
            synchronized (lock) {
                syncl++;
            }
        }));

    }

    public static void testLongAdder() {
        LongAdder adder = new LongAdder();
        execute(TASK_NUM, () -> repeat(INCREMENT_PER_TASK, () -> adder.add(1)));
    }

    public static void repeatWithStatics(int n, Runnable runnable) {
        long[] elapseds = new long[n];

        ntimes(n).forEach(x -> {
            long start = System.currentTimeMillis();
            runnable.run();
            long end = System.currentTimeMillis();
            elapseds[x] = end - start;
        });

        System.out.printf("total: %d, %s\n", Arrays.stream(elapseds).sum(), Arrays.toString(elapseds));
    }

    private static void execute(int n, Runnable task) {
        try {
            CountDownLatch latch = new CountDownLatch(n);
            ExecutorService service = Executors.newFixedThreadPool(100);

            Runnable taskWrapper = () -> {
                task.run();
                latch.countDown();
            };

            service.invokeAll(cloneTask(n, taskWrapper));
            latch.await();
            service.shutdown();
        } catch (Exception e) {}
    }

    private static Collection<Callable<Void>> cloneTask(int n, Runnable task) {
        return ntimes(n).mapToObj(x -> new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                task.run();
                return null;
            }
        }).collect(Collectors.toList());
    }

    private static void repeat(int n, Runnable runnable) {
        ntimes(n).forEach(x -> runnable.run());
    }

    private static IntStream ntimes(int n) {
        return IntStream.range(0, n);
    }

    public static void testMyLongAdder() {
        var al = new MyLongAdder();
        execute(TASK_NUM, () -> repeat(INCREMENT_PER_TASK, () -> al.increment()));
    }

    static class MyLongAdder {
        private static final int LEN = 2 << 5;

        private AtomicLong[] atomicLongs = new AtomicLong[LEN];

        public MyLongAdder() {
            for (int i = 0; i < LEN; ++i) {
                atomicLongs[i] = new AtomicLong(0);
            }
        }

        public void add(long l) {
            atomicLongs[hash(Thread.currentThread()) & (LEN - 1)].addAndGet(l);
        }

        public void increment() {
            add(1);
        }

        public long get() {
            return Arrays.stream(atomicLongs).mapToLong(al -> al.get()).sum();
        }

        // 从HashMap里抄过来的
        private static final int hash(Object key) {
            int h;
            return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
        }
    }
} 