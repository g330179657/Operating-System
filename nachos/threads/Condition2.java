package nachos.threads;

import nachos.machine.*;
import java.util.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock the lock associated with this condition variable.
	 *                      The current thread must hold this lock whenever it uses
	 *                      <tt>sleep()</tt>,
	 *                      <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
	}

	private LinkedList<KThread> waitQueue = new LinkedList<>();

	public int size(){
		return waitQueue.size();
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		boolean intStatus = Machine.interrupt().disable();
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		KThread currentThread = KThread.currentThread();
		waitQueue.add(currentThread);
		conditionLock.release();
		currentThread.sleep();
		conditionLock.acquire();
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		boolean intStatus = Machine.interrupt().disable();
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		KThread next = null;
		while (!waitQueue.isEmpty() && !(next = waitQueue.peek()).isBlocked()){
			waitQueue.poll();
		}
		
		if(!waitQueue.isEmpty()) waitQueue.poll().ready();

		Machine.interrupt().restore(intStatus);

	}

	/**
	* wake up the specific thread in waitQueue and return true if there exist one,
	* otherwise return false.
	*/
	/*public boolean wake(KThread thread){
		boolean intStatus = Machine.interrupt().disable();
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		
		for(int i = 0; i < waitQueue.size(); i++){
			KThread temp = waitQueue.get(i);
			if(temp == thread && temp.isBlocked()){
				temp.ready();
				waitQueue.remove(i);
				return true;
			}
		}

		Machine.interrupt().restore(intStatus);
		return false;
	}
	*/

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		boolean intStatus = Machine.interrupt().disable();
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		while (!waitQueue.isEmpty()) {
			KThread temp = waitQueue.poll();
			if(temp.isBlocked())
				temp.ready();
		}

		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Atomically release the associated lock and go to sleep on
	 * this condition variable until either (1) another thread
	 * wakes it using <tt>wake()</tt>, or (2) the specified
	 * <i>timeout</i> elapses. The current thread must hold the
	 * associated lock. The thread will automatically reacquire
	 * the lock before <tt>sleep()</tt> returns.
	 */
	public void sleepFor(long timeout) {
		boolean intStatus = Machine.interrupt().disable();
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		waitQueue.add(KThread.currentThread());
		conditionLock.release();
		ThreadedKernel.alarm.waitUntil(timeout);
		conditionLock.acquire();
		for(int i = 0; i < waitQueue.size(); i++){
			if(KThread.currentThread() == waitQueue.get(i)){
				waitQueue.remove(i);
				break;
			}
		}
		
		Machine.interrupt().restore(intStatus);
	}

	private Lock conditionLock;



	// Place Condition2 testing code in the Condition2 class.
	public static void selfTest() {
		//sleepForTest1();
		//sleepForTest2();
		//sleepForTest3();
		//sleepForTest4();
		//new InterlockTest();
		//cvTest5();
		//sleepTest1();
		//sleepTest2();
		new wakeTest();
		//wakeTest.wakeTest1();
		//wakeTest.wakeTest2();
		//wakeTest.wakeAllTest1();
    }

	// Example of the "interlock" pattern where two threads strictly
	// alternate their execution with each other using a condition
	// variable. (Also see the slide showing this pattern at the end
	// of Lecture 6.)

	private static class wakeTest {
		private static Lock lock = new Lock();
		private static Condition2 condi2 = new Condition2(lock);

		private static class wakeTestThread implements Runnable {
			public void run() {
				lock.acquire();
				System.out.println(KThread.currentThread().getName() + " go to sleep.");
				condi2.sleep();
				System.out.println(KThread.currentThread().getName() + " wake up.");
				lock.release();
			}
		}

		public static void wakeTest1(){
			KThread thread0 = new KThread(new wakeTestThread());
			KThread thread1 = new KThread(new wakeTestThread());
			KThread check = new KThread(new Runnable() {
				public void run(){
					lock.acquire();
					thread0.setName("Thread 0");
					thread1.setName("Thread 1");
					thread0.fork();
					thread1.fork();
					lock.release();
					ThreadedKernel.alarm.waitUntil(1000);
					lock.acquire();
					System.out.println("Before wake() waitQueue size: " + condi2.size());
					condi2.wake();
					System.out.println("After 1 wake() waitQueue size: " + condi2.size());
					condi2.wake();
					System.out.println("After 2 wake() waitQueue size: " + condi2.size());
					lock.release();
					thread0.join();
					thread1.join();
					lock.acquire();
					lock.release();
				}
			});
			check.fork();
			check.join();
		}

		public static void wakeTest2(){
			KThread thread0 = new KThread(new wakeTestThread());
			KThread thread1 = new KThread(new wakeTestThread());
			KThread check = new KThread(new Runnable() {
				public void run(){
					lock.acquire();
					thread0.setName("Thread 2");
					thread1.setName("Thread 3");
					thread0.fork();
					thread1.fork();
					lock.release();
					ThreadedKernel.alarm.waitUntil(1000);
					lock.acquire();
					System.out.println("Before wake() waitQueue size: " + condi2.size());
					//condi2.wake(thread1);
					//System.out.println("After 1 wake() waitQueue size: " + condi2.size());
					condi2.wake();
					System.out.println("After 2 wake() waitQueue size: " + condi2.size());
					lock.release();
					KThread.yield();
					lock.acquire();

					condi2.wake();
					System.out.println("After 2 wake() waitQueue size: " + condi2.size());
					lock.release();
					KThread.yield();
					lock.acquire();

					condi2.wake();
					System.out.println("After 3 wake() waitQueue size: " + condi2.size());
					lock.release();
					KThread.yield();
					lock.acquire();

					lock.release();
				}
			});
			check.fork();
			check.join();
		}

		public static void wakeAllTest1(){
			KThread thread0 = new KThread(new wakeTestThread());
			KThread thread1 = new KThread(new wakeTestThread());
			KThread check = new KThread(new Runnable() {
				public void run(){
					lock.acquire();
					thread0.setName("Thread 4");
					thread1.setName("Thread 5");
					thread0.fork();
					thread1.fork();
					lock.release();
					ThreadedKernel.alarm.waitUntil(1000);
					lock.acquire();
					System.out.println("Before wakeAll() waitQueue size: " + condi2.size());
					condi2.wakeAll();
					System.out.println("After wakeAll() waitQueue size: " + condi2.size());
					lock.release();
					thread0.join();
					thread1.join();
					lock.acquire();
					lock.release();
				}
			});
			check.fork();
			check.join();
		}

	}

	public static void sleepTest1(){
		Lock lock = new Lock();
		Condition2 cv = new Condition2(lock);
		lock.acquire();
		KThread curr = KThread.currentThread();
		KThread check = new KThread(new Runnable(){
			public void run(){
				lock.acquire();
				Lib.assertTrue(curr.isBlocked(), "The Running thread should be blocked.");
				System.out.println("The calling thread of sleep() is blocked!");
				cv.wake();
				lock.release();
			}
		});
		
		check.fork();
		cv.sleep();
		System.out.println("wake up!");
		lock.release();
	}

	public static void sleepTest2(){
		Lock lock = new Lock();
		Condition2 cv = new Condition2(lock);
		lock.acquire();
		KThread curr = KThread.currentThread();
		KThread check = new KThread(new Runnable(){
			public void run(){
				lock.acquire();
				Lib.assertTrue(curr.isBlocked(), "The Running thread should be blocked.");
				System.out.println("The main thread should be blocked! is it?" + curr.isBlocked());
				cv.wake();
				lock.release();
			}
		});
		System.out.println("Before call wake and wakeAll, waitQueue size: " + cv.size());
		cv.wake();
		System.out.println("Called wake(), waitQueue size: " + cv.size());
		cv.wakeAll();
		System.out.println("Called wakeAll(), waitQueue size: " + cv.size());

		check.fork();
		cv.sleep();
		System.out.println("The main thread wakes up!");
		lock.release();
	}

	// Example of the "interlock" pattern where two threads strictly
	// alternate their execution with each other using a condition
	// variable. (Also see the slide showing this pattern at the end
	// of Lecture 6.)

	private static class InterlockTest {
		private static Lock lock;
		private static Condition2 cv;

		private static class Interlocker implements Runnable {
			public void run() {
				lock.acquire();
				for (int i = 0; i < 10; i++) {
					System.out.println(KThread.currentThread().getName());
					cv.wake(); // signal
					cv.sleep(); // wait
				}
				lock.release();
			}
		}

		public InterlockTest() {
			lock = new Lock();
			cv = new Condition2(lock);

			KThread ping = new KThread(new Interlocker());
			ping.setName("ping");
			KThread pong = new KThread(new Interlocker());
			pong.setName("pong");

			ping.fork();
			pong.fork();

			// We need to wait for ping to finish, and the proper way
			// to do so is to join on ping. (Note that, when ping is
			// done, pong is sleeping on the condition variable; if we
			// were also to join on pong, we would block forever.)
			// For this to work, join must be implemented. If you
			// have not implemented join yet, then comment out the
			// call to join and instead uncomment the loop with
			// yields; the loop has the same effect, but is a kludgy
			// way to do it.
			ping.join();
			// for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
			System.out.println("After interlocktest size: " + cv.size());
		}

	}

	public static void cvTest5() {
		final Lock lock = new Lock();
		// final Condition empty = new Condition(lock);
		final Condition2 empty = new Condition2(lock);
		final LinkedList<Integer> list = new LinkedList<>();

		KThread consumer = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				while (list.isEmpty()) {
					empty.sleep();
				}
				Lib.assertTrue(list.size() == 5, "List should have 5 values.");
				while (!list.isEmpty()) {
					// context swith for the fun of it
					KThread.currentThread().yield();
					System.out.println("Removed " + list.removeFirst());
				}
				lock.release();
			}
		});

		KThread producer = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				for (int i = 0; i < 5; i++) {
					list.add(i);
					System.out.println("Added " + i);
					// context swith for the fun of it
					empty.wake();
					KThread.currentThread().yield();
				}
				empty.wake();
				lock.release();
			}
		});

		consumer.setName("Consumer");
		producer.setName("Producer");
		consumer.fork();
		producer.fork();

		// We need to wait for the consumer and producer to finish,
		// and the proper way to do so is to join on them. For this
		// to work, join must be implemented. If you have not
		// implemented join yet, then comment out the calls to join
		// and instead uncomment the loop with yield; the loop has the
		// same effect, but is a kludgy way to do it.
		consumer.join();
		producer.join();
		// for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
		System.out.println("After cvtest5 size: " + empty.size());

	}

	    // Place sleepFor test code inside of the Condition2 class.

	private static void sleepForTest1 () {
		Lock lock = new Lock();
		Condition2 cv = new Condition2(lock);

		lock.acquire();
		long t0 = Machine.timer().getTime();
		System.out.println (KThread.currentThread().getName() + " sleeping");
		// no other thread will wake us up, so we should time out
		cv.sleepFor(2000);
		long t1 = Machine.timer().getTime();
		System.out.println (KThread.currentThread().getName() +
					" woke up, slept for " + (t1 - t0) + " ticks");
		System.out.println("after sleep for test 1 Size: " + cv.size());
		lock.release();
    }

    private static void sleepForTest2 () {
		KThread curr = KThread.currentThread();
		KThread waker = new KThread(new Runnable() {
			public void run(){
				ThreadedKernel.alarm.waitUntil(500);
				System.out.println("Call cancel: " + ThreadedKernel.alarm.cancel(curr));
			} 
		});

		Lock lock = new Lock();
		Condition2 cv = new Condition2(lock);

		lock.acquire();
		long t0 = Machine.timer().getTime();
		System.out.println (KThread.currentThread().getName() + " sleeping");

		waker.fork();
		cv.sleepFor(2000);
		long t1 = Machine.timer().getTime();
		System.out.println (KThread.currentThread().getName() +
				    " woke up, slept for " + (t1 - t0) + " ticks");
		System.out.println("after sleep for test 2 Size: " + cv.size());
		lock.release();
    }

	private static void sleepForTest3(){
		KThread curr = KThread.currentThread();

		Lock lock = new Lock();
		Condition2 cv = new Condition2(lock);
		KThread thread0 = new KThread(new Runnable(){
			public void run(){
				lock.acquire();
				long t0 = Machine.timer().getTime();
				System.out.println(KThread.currentThread().getName() + " sleeping");
				cv.sleepFor(5000);
				long t1 = Machine.timer().getTime();
				System.out.println (KThread.currentThread().getName() +
				    " woke up, slept for " + (t1 - t0) + " ticks");
				lock.release();
			}
		});
		thread0.setName("Thread0");
		KThread thread1 = new KThread(new Runnable(){
			public void run(){
				lock.acquire();
				long t0 = Machine.timer().getTime();
				System.out.println(KThread.currentThread().getName() + " sleeping");
				cv.sleepFor(10000);
				long t1 = Machine.timer().getTime();
				System.out.println (KThread.currentThread().getName() +
				    " woke up, slept for " + (t1 - t0) + " ticks");
				lock.release();
			}
		});
		thread1.setName("Thread1");
		KThread thread2 = new KThread(new Runnable(){
			public void run(){
				lock.acquire();
				long t0 = Machine.timer().getTime();
				System.out.println(KThread.currentThread().getName() + " sleeping");
				cv.sleepFor(100000);
				long t1 = Machine.timer().getTime();
				System.out.println (KThread.currentThread().getName() +
				    " woke up, slept for " + (t1 - t0) + " ticks");
				lock.release();
			}
		});
		thread2.setName("Thread2");

		thread0.fork();
		thread1.fork();
		thread2.fork();
		ThreadedKernel.alarm.waitUntil(500);
		lock.acquire();
		cv.wakeAll();
		lock.release();
		thread2.join();
	}

	private static void sleepForTest4(){
		KThread curr = KThread.currentThread();

		Lock lock = new Lock();
		Condition2 cv = new Condition2(lock);
		KThread thread0 = new KThread(new Runnable(){
			public void run(){
				lock.acquire();
				long t0 = Machine.timer().getTime();
				System.out.println(KThread.currentThread().getName() + " sleeping");
				cv.sleepFor(5000);
				long t1 = Machine.timer().getTime();
				System.out.println (KThread.currentThread().getName() +
				    " woke up, slept for " + (t1 - t0) + " ticks");
				lock.release();
			}
		});
		thread0.setName("Thread0");
		KThread thread1 = new KThread(new Runnable(){
			public void run(){
				lock.acquire();
				long t0 = Machine.timer().getTime();
				System.out.println(KThread.currentThread().getName() + " sleeping");
				cv.sleepFor(10000);
				long t1 = Machine.timer().getTime();
				System.out.println (KThread.currentThread().getName() +
				    " woke up, slept for " + (t1 - t0) + " ticks");
				lock.release();
			}
		});
		thread1.setName("Thread1");
		KThread thread2 = new KThread(new Runnable(){
			public void run(){
				lock.acquire();
				long t0 = Machine.timer().getTime();
				System.out.println(KThread.currentThread().getName() + " sleeping");
				cv.sleepFor(100000);
				long t1 = Machine.timer().getTime();
				System.out.println (KThread.currentThread().getName() +
				    " woke up, slept for " + (t1 - t0) + " ticks");
				lock.release();
			}
		});
		thread2.setName("Thread2");

		thread0.fork();
		thread1.fork();
		thread2.fork();
		ThreadedKernel.alarm.waitUntil(500);
		lock.acquire();
		cv.wake();
		cv.wake();
		lock.release();
		thread2.join();
	}




}
