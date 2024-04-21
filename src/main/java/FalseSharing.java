/**
 *
 *
 * VolatileLong : duration = 38541253166
 *VolatileLong2 : duration = 3984175305
 * 不到10倍的提升
 *
 * https://www.iteye.com/blog/budairenqin-2048257
 *
 * https://mail.openjdk.org/pipermail/hotspot-dev/2012-November/007309.html
 *
 * -XX:+PrintFieldLayout
 *
 *
 * C. Marking multiple fields makes each field padded:
 *
 *     public static class ContendedTest4 {
 *         @Contended
 *         private Object contendedField1;
 *
 *         @Contended
 *         private Object contendedField2;
 *
 *         private Object plainField3;
 *         private Object plainField4;
 *     }
 *
 * ...pushes both fields with individual padding for each:
 *
 *    TestContended$ContendedTest4: field layout
 *      @ 12 --- instance fields start ---
 *      @ 12 "plainField3" Ljava.lang.Object;
 *      @ 16 "plainField4" Ljava.lang.Object;
 *      @148 "contendedField1" Ljava.lang.Object; (contended, group = 0)
 *      @280 "contendedField2" Ljava.lang.Object; (contended, group = 0)
 *      @416 --- instance fields end ---
 *      @416 --- instance ends ---
 *
 *
 *   Contention groups So that:
 *
 *     public static class ContendedTest5 {
 *         @Contended("updater1")
 *         private Object contendedField1;
 *
 *         @Contended("updater1")
 *         private Object contendedField2;
 *
 *         @Contended("updater2")
 *         private Object contendedField3;
 *
 *         private Object plainField5;
 *         private Object plainField6;
 *     }
 *
 * ...is laid out as:
 *
 *    TestContended$ContendedTest5: field layout
 *      @ 12 --- instance fields start ---
 *      @ 12 "plainField5" Ljava.lang.Object;
 *      @ 16 "plainField6" Ljava.lang.Object;
 *      @148 "contendedField1" Ljava.lang.Object; (contended, group = 12)
 *      @152 "contendedField2" Ljava.lang.Object; (contended, group = 12)
 *      @284 "contendedField3" Ljava.lang.Object; (contended, group = 15)
 *      @416 --- instance fields end ---
 *      @416 --- instance ends ---
 */
public class FalseSharing implements Runnable {

    public final static int NUM_THREADS = 4; // change  
    public final static long ITERATIONS = 500L * 1000L * 1000L;
    private final int arrayIndex;

    private static VolatileLong2[] longs = new VolatileLong2[NUM_THREADS];
    static {
        for (int i = 0; i < longs.length; i++) {
            longs[i] = new VolatileLong2();
        }
    }

    public FalseSharing(final int arrayIndex) {
        this.arrayIndex = arrayIndex;
    }

    public static void main(final String[] args) throws Exception {
        long start = System.nanoTime();
        runTest();
        System.out.println("duration = " + (System.nanoTime() - start));
    }

    private static void runTest() throws InterruptedException {
        Thread[] threads = new Thread[NUM_THREADS];

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new FalseSharing(i));
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }
    }

    public void run() {
        long i = ITERATIONS + 1;
        while (0 != --i) {
            longs[arrayIndex].value = i;
        }
    }

    public final static class VolatileLong {
        public volatile long value = 0L;
    }

    // long padding避免false sharing  
    // 按理说jdk7以后long padding应该被优化掉了，但是从测试结果看padding仍然起作用
    // 因为用7个下标保证了会有56个字节填充在数值的任何一边，56字节的填充+8字节的long数值正好装进一行64字节的缓存行。
    public final static class VolatileLong2 {
        volatile long p0, p1, p2, p3, p4, p5, p6;
        public volatile long value = 0L;
        volatile long q0, q1, q2, q3, q4, q5, q6;
    }

    //// jdk8新特性，Contended注解避免false sharing
    //// Restricted on user classpath
    //// Unlock: -XX:-RestrictContended
    //@sun.misc.Contended
    //public final static class VolatileLong3 {
    //    public volatile long value = 0L;
    //}
}  