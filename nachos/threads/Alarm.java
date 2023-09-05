package nachos.threads;

import nachos.machine.*;
import java.util.*;
/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
  	class LocalThread {
    		KThread thread;
    		long waketime;
    
    		LocalThread(KThread kthread, long time){
      			thread = kthread;
      			waketime = time;
    		}
  	}
  
  	private static Queue<LocalThread> sleepQueue = new PriorityQueue<>(new Comparator<LocalThread>(){
    		public int compare(LocalThread l1, LocalThread l2){
      			return l1.waketime >= l2.waketime ? 1 : -1;
				//return (int)(l1.waketime - l2.waketime);
				}
  		});
  
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		LocalThread temp;
		if(sleepQueue.size() == 0){
			temp = null;
		}else{
			temp = sleepQueue.peek();
		}
		Lib.debug('a', "current time: " + Machine.timer().getTime());
		while(temp != null && temp.waketime <= Machine.timer().getTime()){
			if(temp.thread.isBlocked()){
				temp.thread.ready();
			}
			
			Lib.debug('a', "After ready: " + Machine.timer().getTime());
			sleepQueue.poll();
			temp = sleepQueue.size() == 0 ? null : sleepQueue.peek();
		}
		KThread.currentThread().yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		if(x <= 0) return;
		boolean intStatus = Machine.interrupt().disable();
		Lib.debug('a', "Before sleep: " + Machine.timer().getTime());
		LocalThread lt = new LocalThread(KThread.currentThread(), Machine.timer().getTime() + x);
		sleepQueue.add(lt);
		Lib.debug('a', "waketime of LocalThread lt: " + lt.waketime);
    	KThread.sleep();
    	Machine.interrupt().restore(intStatus);
		// for now, cheat just to get something working (busy waiting is bad)
		// long wakeTime = Machine.timer().getTime() + x;
		// while (wakeTime > Machine.timer().getTime())
		// 	KThread.yield();
	}

        /**
	 * Cancel any timer set by <i>thread</i>, effectively waking
	 * up the thread immediately (placing it in the scheduler
	 * ready set) and returning true.  If <i>thread</i> has no
	 * timer set, return false.
	 * 
	 * <p>
	 * @param thread the thread whose timer should be cancelled.
	 */
        public boolean cancel(KThread thread) {
			boolean intStatus = Machine.interrupt().disable();

			KThread temp = null;
			for(LocalThread lt : sleepQueue){
				if(lt.thread == thread && thread.isBlocked()){
					thread.ready();
					Machine.interrupt().restore(intStatus);
					return true;
				}
			}


			Machine.interrupt().restore(intStatus);
			return false;
	}

	    	// Add Alarm testing code to the Alarm class
    
    public static void alarmTest1() {
		int durations[] = {10*1000, 1000, 100*1000};
		long t0, t1;

		for (int d : durations) {
	    	t0 = Machine.timer().getTime();
	    	ThreadedKernel.alarm.waitUntil(d);
	    	t1 = Machine.timer().getTime();

    		System.out.println ("alarmTest1: waited for " + (t1 - t0) + " ticks");
		}
    }

	public static void alarmTest2(){
		KThread thread0 = new KThread(new Runnable(){
			public void run(){
				long t0 = Machine.timer().getTime();
				ThreadedKernel.alarm.waitUntil(10000);
				long t1 = Machine.timer().getTime();

				System.out.println (KThread.currentThread().getName() + " waited for " + (t1 - t0) + " ticks");
			}
		});
		thread0.setName("Thread0");
		KThread thread1 = new KThread(new Runnable(){
			public void run(){
				long t0 = Machine.timer().getTime();
				ThreadedKernel.alarm.waitUntil(4000);
				long t1 = Machine.timer().getTime();

				System.out.println (KThread.currentThread().getName() + " waited for " + (t1 - t0) + " ticks");
			}
		});
		thread1.setName("Thread1");
		KThread thread2 = new KThread(new Runnable(){
			public void run(){
				long t0 = Machine.timer().getTime();
				ThreadedKernel.alarm.waitUntil(8000);
				long t1 = Machine.timer().getTime();

				System.out.println (KThread.currentThread().getName() + " waited for " + (t1 - t0) + " ticks");
			}
		});
		thread2.setName("Thread2");
		thread1.fork();thread2.fork();thread0.fork();
		thread0.join();

	}

    		// Implement more test methods here ...

    		// Invoke Alarm.selfTest() from ThreadedKernel.selfTest()
    	public static void selfTest() {
			//alarmTest1();
			alarmTest2();
		// Invoke your other test methods here ...
    	}
	


}
