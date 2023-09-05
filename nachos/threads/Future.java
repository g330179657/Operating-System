package nachos.threads;

import java.util.*;
import java.util.function.IntSupplier;
import nachos.machine.*;

/**
 * A <i>Future</i> is a convenient mechanism for using asynchonous
 * operations.
 */
public class Future {
    private Integer result = null;
    private Lock lock = new Lock();
    private Condition2 cv = new Condition2(lock);
    /**
     * Instantiate a new <i>Future</i>.  The <i>Future</i> will invoke
     * the supplied <i>function</i> asynchronously in a KThread.  In
     * particular, the constructor should not block as a consequence
     * of invoking <i>function</i>.
     */
    public Future (IntSupplier function) {
        KThread fun = new KThread(new Runnable(){
            public void run(){
                result = function.getAsInt();
                lock.acquire();
                cv.wakeAll();
                lock.release();
            }
        });
        fun.fork();
    }

    /**
     * Return the result of invoking the <i>function</i> passed in to
     * the <i>Future</i> when it was created.  If the function has not
     * completed when <i>get</i> is invoked, then the caller is
     * blocked.  If the function has completed, then <i>get</i>
     * returns the result of the function.  Note that <i>get</i> may
     * be called any number of times (potentially by multiple
     * threads), and it should always return the same value.
     */
    public int get () {
        if(result != null){
            return result;
        }

        lock.acquire();
        cv.sleep();
        lock.release();
        return result;
    }


    public static void selfTest(){
        futureTest1();
    }

    private static void futureTest1(){
        IntSupplier sup = () -> {
            ThreadedKernel.alarm.waitUntil(20000);
            return (int)(Math.random() * 10);
        };
        Future f = new Future(sup);
        KThread thread0 = new KThread(new Runnable(){
            public void run(){
                long t0 = Machine.timer().getTime();
                int res = f.get();
                long t1 = Machine.timer().getTime();
                System.out.println("Thread0 get the value " + res + " in " + (t1-t0) + " ticks.");
            }
        });
        KThread thread1 = new KThread(new Runnable(){
            public void run(){
                long t0 = Machine.timer().getTime();
                int res = f.get();
                long t1 = Machine.timer().getTime();
                System.out.println("Thread1 get the value " + res + " in " + (t1-t0) + " ticks.");
            }
        });

        thread0.fork();
        thread0.join();
        thread1.fork();
        thread1.join();

    }
}
