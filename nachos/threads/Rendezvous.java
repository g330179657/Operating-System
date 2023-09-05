package nachos.threads;

import nachos.machine.*;
import java.util.*;
/**
 * A <i>Rendezvous</i> allows threads to synchronously exchange values.
 */
public class Rendezvous {
    /**
     * Allocate a new Rendezvous.
     */
    public Rendezvous () {
        //lock = new Lock();
        //condition = new Condition2(lock);
    }

    class ExchangeObject {
        private Lock lock;
        private Condition2 cv;
        private Lock gateLock;
        private Condition2 gateCv;
        private int size;
        private int value;
        public int waitNumber;
        public ExchangeObject(){
            this.lock = new Lock();
            this.cv = new Condition2(lock);
            this.gateLock = new Lock();
            this.gateCv = new Condition2(gateLock);
            this.size = 0;
            this.waitNumber = 0;
        }
        public Condition2 cv(){
            return this.cv;
        }
        public Condition2 gateCv(){
            return this.gateCv;
        }
        public Lock lock(){
            return this.lock;
        }
        public Lock gateLock(){
            return this.gateLock;
        }
        public void setSize(int i){
            this.size = i;
        }
        public int size(){
            return this.size;
        }
        public void setValue(int v){
            this.value = v;
        }
        public int getValue(){
            return value;
        }
    }
    // <tag, ExchangeObject>
    private Map<Integer, ExchangeObject> map = new HashMap<>();
    private Lock nameLock = new Lock();
    /**
     * Synchronously exchange a value with another thread.  The first
     * thread A (with value X) to exhange will block waiting for
     * another thread B (with value Y).  When thread B arrives, it
     * will unblock A and the threads will exchange values: value Y
     * will be returned to thread A, and value X will be returned to
     * thread B.
     *
     * Different integer tags are used as different, parallel
     * synchronization points (i.e., threads synchronizing at
     * different tags do not interact with each other).  The same tag
     * can also be used repeatedly for multiple exchanges.
     *
     * @param tag the synchronization tag.
     * @param value the integer to exchange.
     */
    public int exchange (int tag, int value) {
        nameLock.acquire();
        if(!map.containsKey(tag)){
            map.put(tag, new ExchangeObject());
        }
        ExchangeObject eo = map.get(tag);
        nameLock.release();

        eo.gateLock().acquire();
        if(eo.size() == 2 || eo.waitNumber > 0){
            eo.waitNumber++;
            eo.gateCv().sleep();
            eo.waitNumber--;
        }
        eo.lock().acquire();
        if(eo.size() == 0){
            eo.setValue(value);
            eo.setSize(1);
            eo.gateLock().release();
            eo.cv().sleep();
            eo.gateLock().acquire();
            int res = eo.getValue();
            eo.setSize(0);
            eo.lock().release();
            if(eo.waitNumber > 0){
                //eo.waitNumber--;
                eo.gateCv().wake();
                if(eo.waitNumber > 0){
                    //eo.waitNumber--;
                    eo.gateCv().wake();
                }
            }
            eo.gateLock().release();
            return res;
        }else{
            int res = eo.getValue();
            eo.setValue(value);
            eo.setSize(2);
            eo.cv().wake();
            eo.lock().release();
            eo.gateLock().release();
            return res;
        }
        





        //eo.gateLock().release();


        /*lock.acquire();
        if(!map.containsKey(tag)){
            map.put(tag, new ExchangeThread(KThread.currentThread(), value));
            condition.sleep();
            int ret = finishedPair.get(KThread.currentThread());
            finishedPair.remove(KThread.currentThread());
            lock.release();
            return ret;
        }else{
            ExchangeThread pairThread = map.get(tag);
            map.remove(tag);
            finishedPair.put(pairThread.thread, value);
            condition.wake(pairThread.thread);
            lock.release();
            return pairThread.sent;
        }*/
        
    }

        // Place Rendezvous test code inside of the Rendezvous class.

    public static void rendezTest1() {
        final Rendezvous r = new Rendezvous();

        KThread t1 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t1.setName("t1");
        KThread t2 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t2.setName("t2");

        t1.fork(); t2.fork();
        // assumes join is implemented correctly
        t1.join(); t2.join();
    }

    public static void rendezTest2() {
        final Rendezvous r = new Rendezvous();

        KThread t1 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t1.setName("t1");
        KThread t2 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t2.setName("t2");
        KThread t3 = new KThread( new Runnable () {
            public void run() {
                int tag = 2;
                int send = 1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t3.setName("t3");
        KThread t4 = new KThread( new Runnable () {
            public void run() {
                int tag = 2;
                int send = -1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t4.setName("t4");

        t1.fork(); t2.fork(); t3.fork();
        // assumes join is implemented correctly
        //t1.join(); t2.join(); t3.join();
        for(int i = 0; i < 1000; i++){
            KThread.yield();
        }
        System.out.println("1000 years later...");
        t4.fork();
        t4.join();

    }

    public static void rendezTest3() {
        final Rendezvous r = new Rendezvous();

        KThread t1 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t1.setName("t1");
        KThread t2 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t2.setName("t2");
        KThread t3 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 2;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -2, "Was expecting " + -2 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t3.setName("t3");
        KThread t4 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -2;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 2, "Was expecting " + 2 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t4.setName("t4");
        KThread t5 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 3;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -3, "Was expecting " + -3 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t5.setName("t5");
        KThread t6 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -3;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 3, "Was expecting " + 3 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t6.setName("t6");

        t1.fork(); t2.fork(); t3.fork(); t4.fork(); t5.fork(); t6.fork();
        // assumes join is implemented correctly
        t1.join(); t2.join(); t3.join(); t4.join(); t5.join(); t6.join();
    }

    public static void rendezTest4() {
        final Rendezvous r1 = new Rendezvous();
        final Rendezvous r2 = new Rendezvous();
        KThread t1 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r1.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t1.setName("t1");
        KThread t2 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r1.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t2.setName("t2");
        KThread t3 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 2;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r2.exchange (tag, send);
                Lib.assertTrue (recv == -2, "Was expecting " + -2 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t3.setName("t3");
        KThread t4 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -2;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r2.exchange (tag, send);
                Lib.assertTrue (recv == 2, "Was expecting " + 2 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t4.setName("t4");

        t1.fork(); t3.fork(); t2.fork(); t4.fork();
        // assumes join is implemented correctly
        t1.join(); t2.join(); t3.join(); t4.join();
    }

        // Invoke Rendezvous.selfTest() from ThreadedKernel.selfTest()

    public static void selfTest() {
	    // place calls to your Rendezvous tests that you implement here
	    //rendezTest1();
        //rendezTest2();
        //rendezTest3();
        rendezTest4();

    }

}
