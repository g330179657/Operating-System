package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
		swapFile = ThreadedKernel.fileSystem.open("swapFile", true);
		currSwapFileSize = 10;
		freeSwapPages = new LinkedList<>();
		for(int i = 0; i < currSwapFileSize; i++){
			freeSwapPages.add(i);
		}
		swapPagesMap = new HashMap<>();
		pinLock = new Lock();
		pinCon = new Condition(pinLock);
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

	public static boolean isFull(){
		for(int i = 0; i < numPhysPages; i++){
			if(invertedPageTable[i] == null || clockList[i] == null){
				System.out.println(clockList[i] == null);
				return false;
			}
		}
		return true;
	}

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';

	public static OpenFile swapFile;

	public static int currSwapFileSize;

	public static LinkedList<Integer> freeSwapPages;
	//	Map<UserProcess, Map<vpn, spn>>
	public static Map<UserProcess, Map<Integer, Integer>> swapPagesMap;

	public static int numPhysPages = Machine.processor().getNumPhysPages();

	public static UserProcess[] invertedPageTable = new UserProcess[numPhysPages];
	//	Set<PhysicalPage>
	public static Set<Integer> pinnedPage = new HashSet<>();

	public static Integer clock = 0;

	public static TranslationEntry[] clockList = new TranslationEntry[numPhysPages];

	public static Lock pinLock;

	public static Condition pinCon;
}
