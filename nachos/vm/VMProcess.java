package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.io.*;
import java.util.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
		VMKernel.swapPagesMap.put(this, new HashMap<Integer, Integer>());
	}

	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		//System.out.println("length: " + length);
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);
		byte[] memory = Machine.processor().getMemory();

		int amount = 0;
		int remain = length;
		if(vaddr < 0 || vaddr/pageSize >= memory.length){
			//System.out.println("vaddr: " + vaddr + " memory.length: " + memory.length);
			return 0;
		}
		try{
			VMKernel.pinLock.acquire();
			while(remain > 0){
				int vpn = vaddr / pageSize;
				int voff = vaddr % pageSize;
				if(vpn >= pageTable.length){
					VMKernel.pinLock.release();
					return amount;
				}
				TranslationEntry te = pageTable[vpn];
				if(te.valid == false){
					handlePageFault(vaddr);
				}
				Lib.assertTrue(!VMKernel.pinnedPage.contains(te.ppn), "this page should not be pinned.");
				VMKernel.pinnedPage.add(te.ppn);
				int ppn = te.ppn;
				te.used = true;
				int n = (remain < pageSize - voff) ? remain : pageSize-voff;
				System.arraycopy(memory, ppn*pageSize + voff, data, offset, n);
				vaddr += n;
				offset += n;
				remain -= n;
				amount += n;
				VMKernel.pinnedPage.remove(te.ppn);
				VMKernel.pinCon.wakeAll();
			}
			//System.out.println("length: " + length + " amount: " + amount);
			VMKernel.pinLock.release();
			return amount;
		}
		catch(Exception e){
			System.out.println(e.getMessage());
			return amount;
		}
	}

	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		int amount = 0;
		int remain = length;
		if(vaddr < 0 || vaddr/pageSize >= memory.length){
			return 0;
		}
		try{
			VMKernel.pinLock.acquire();
			while(remain > 0){
				int vpn = vaddr / pageSize;
				int voff = vaddr % pageSize;
				if(vpn >= pageTable.length){
					VMKernel.pinLock.release();
					return amount;
				}
				TranslationEntry te = pageTable[vpn];
				if(te.valid == false){
					handlePageFault(vaddr);
				}
				if(te.readOnly){
					VMKernel.pinLock.release();
					return amount;
				}
				Lib.assertTrue(!VMKernel.pinnedPage.contains(te.ppn), "this page should not be pinned.");
				VMKernel.pinnedPage.add(te.ppn);
				int ppn = te.ppn;
				te.used = true;
				te.dirty = true;
				int n = (remain < pageSize - voff) ? remain : pageSize-voff;
				System.arraycopy(data, offset, memory, ppn*pageSize + voff, n);
				vaddr += n;
				offset += n;
				remain -= n;
				amount += n;
				VMKernel.pinnedPage.remove(te.ppn);
				VMKernel.pinCon.wakeAll();
			}
			VMKernel.pinLock.release();
			return amount;
		}
		catch(Exception e){
			VMKernel.pinLock.release();
			System.out.println(e.getMessage());
			return amount;
		}
	}


	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		super.saveState();
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		super.restoreState();
	}

	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {
		//return super.loadSections();
		pageTable = new TranslationEntry[numPages];
		for(int i = 0; i < numPages; i++){
			pageTable[i] = new TranslationEntry();
		}
		//VMKernel.swapPagesMap.put(this, new HashMap<Integer, Integer>());
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		//super.unloadSections();
		VMKernel.pageLock.acquire();
		Map<Integer, Integer> map = VMKernel.swapPagesMap.get(this);
		for(int k : map.keySet()){
			int spn = map.get(k);
			VMKernel.freeSwapPages.add(spn);
		}
		VMKernel.swapPagesMap.remove(this);
		for(int i = 0; i < pageTable.length; i++){
			TranslationEntry te = pageTable[i];
			if(te.valid){
				VMKernel.freePhysPages.add(te.ppn);
				VMKernel.invertedPageTable[te.ppn] = null;
				VMKernel.clockList[te.ppn] = null;
			}
		}
		VMKernel.pageLock.release();
	}

	public Integer nextFreePhysPage(){
		
		VMKernel.pageLock.acquire();
		while(VMKernel.pinnedPage.size() == VMKernel.numPhysPages){
			VMKernel.pinCon.sleep();
		}
		if(VMKernel.freePhysPages.size() > 0){
			int ppn = VMKernel.freePhysPages.poll();
			VMKernel.pageLock.release();
			return ppn;
		}
		//Lib.assertTrue(VMKernel.isFull(), "The InvertedPageTable and the clockList are not full.");
		Lib.assertTrue(VMKernel.pinnedPage.size() < VMKernel.numPhysPages, "Pinned too much pages.");
		// clock algorithm
		for(int i = 0; i <= 2*VMKernel.numPhysPages; i++){
			VMKernel.clock = (VMKernel.clock+1) % VMKernel.numPhysPages;
			int ppn = VMKernel.clock;
			if(VMKernel.pinnedPage.contains(ppn) || VMKernel.clockList[ppn] == null || VMKernel.invertedPageTable[ppn] == null){
				continue;
			}
			TranslationEntry te = VMKernel.clockList[ppn];
			if(te.used){
				te.used = false;
				//VMKernel.clock++;
			}
			else if(te.readOnly || !te.dirty){
				te.valid = false;
				VMKernel.invertedPageTable[ppn] = null;
				VMKernel.clockList[ppn] = null;
				VMKernel.clock++;
				VMKernel.pageLock.release();
				return ppn;
			}
			else{
				// swap this page out
				if(VMKernel.freeSwapPages.size() == 0){
					for(int j = 0; j < 10; j++){
						VMKernel.freeSwapPages.add(VMKernel.currSwapFileSize+j);
					}
					VMKernel.currSwapFileSize += 10;
				}
				int spn = VMKernel.freeSwapPages.poll();
				byte[] buffer = new byte[pageSize];
				byte[] memory = Machine.processor().getMemory();
				System.arraycopy(memory, ppn*pageSize, buffer, 0, pageSize);
				VMKernel.swapFile.write(spn*pageSize, buffer, 0, pageSize);
				UserProcess up = VMKernel.invertedPageTable[ppn];
				VMKernel.swapPagesMap.get(up).put(te.vpn, spn);
				te.valid = false;
				VMKernel.invertedPageTable[ppn] = null;
				VMKernel.clockList[ppn] = null;
				VMKernel.clock++;
				VMKernel.pageLock.release();
				return ppn;
			}
		}
		Lib.assertNotReached("ALL Pinned?");
		VMKernel.pageLock.release();
		return -1;
	}

	/**
	 * demand page request. 
	 */
	public boolean prepareRequestedPageFromCoff(int vpn){
		Lib.assertTrue(pageTable[vpn].valid == false, "Already valid, no need to prepare.");
		//System.out.println("Test");
		//String coffName = thread.getName();
		//System.out.println("coffName: " + coffName);
		OpenFile executable = ThreadedKernel.fileSystem.open(coffName, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}
		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}
		
		int ppn = nextFreePhysPage();
		//System.out.print(" given ppn: " + ppn + "\n");
		Lib.assertTrue(VMKernel.clockList[ppn] == null, "The clock list #" + ppn + " should be null");
		for(int s = 0; s < coff.getNumSections(); s++){
			CoffSection section = coff.getSection(s);
			for(int j = 0; j < section.getLength(); j++){
				int i = section.getFirstVPN() + j;
				if(vpn == i){
					section.loadPage(j, ppn);
					pageTable[vpn].vpn = vpn;
					pageTable[vpn].ppn = ppn;
					pageTable[vpn].valid = true;
					pageTable[vpn].readOnly = section.isReadOnly();
					pageTable[vpn].used = false;
					pageTable[vpn].dirty = false;
					VMKernel.invertedPageTable[ppn] = this;
					VMKernel.clockList[ppn] = pageTable[vpn];
					coff.close();
					return true;
				}
			}
		}
		zeroFill(ppn);
		pageTable[vpn].vpn = vpn;
		pageTable[vpn].ppn = ppn;
		pageTable[vpn].valid = true;
		pageTable[vpn].readOnly = false;
		pageTable[vpn].used = false;
		pageTable[vpn].dirty = true;
		VMKernel.invertedPageTable[ppn] = this;
		VMKernel.clockList[ppn] = pageTable[vpn];
		coff.close();
		return true;
	}
	private boolean zeroFill(int ppn){
		byte[] memory = Machine.processor().getMemory();
		Arrays.fill(memory, ppn*pageSize, (ppn+1)*pageSize, (byte)0);
		return true;
	}

	public boolean prepareRequestedPageFromSwapFile(int vpn){
		int ppn = nextFreePhysPage();
		//System.out.print(" given ppn: " + ppn + "\n");
		Lib.assertTrue(VMKernel.clockList[ppn] == null, "The clock list #" + ppn + " should be null.");
		/*for(int k : VMKernel.swapPagesMap.get(this).keySet()){
			System.out.println("vpn: " + k + " spn: " + VMKernel.swapPagesMap.get(this).get(k));
		}*/
		int spn = VMKernel.swapPagesMap.get(this).get(vpn);
		byte[] buffer = new byte[pageSize];
		VMKernel.swapFile.read(spn*pageSize, buffer, 0, pageSize);
		byte[] memory = Machine.processor().getMemory();
		System.arraycopy(buffer, 0, memory, ppn*pageSize, pageSize);
		pageTable[vpn].vpn = vpn;
		pageTable[vpn].ppn = ppn;
		pageTable[vpn].valid = true;
		pageTable[vpn].readOnly = false;
		pageTable[vpn].used = false;
		pageTable[vpn].dirty = true;
		VMKernel.swapPagesMap.get(this).remove(vpn);
		VMKernel.freeSwapPages.add(spn);
		VMKernel.invertedPageTable[ppn] = this;
		VMKernel.clockList[ppn] = pageTable[vpn];
		return true;
	}

	/**
	 * Handle page fault.
	 */
	public int handlePageFault(int vaddr){
		int vpn = vaddr / pageSize;
		//System.out.print("\nhandle fault vpn: " + vpn);
		Lib.assertTrue(pageTable[vpn].valid == false, "Already valid, no page fault.");
		if(VMKernel.swapPagesMap.containsKey(this) && VMKernel.swapPagesMap.get(this).containsKey(vpn)){
			//System.out.print(" swapfile ");
			prepareRequestedPageFromSwapFile(vpn);
			return 0;
		}
		//System.out.print(" coffFile ");
		prepareRequestedPageFromCoff(vpn);
		return 1;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionPageFault:
			int result = handlePageFault(processor.readRegister(Processor.regBadVAddr));
			break;
		default:
			super.handleException(cause);
			break;
		}
	}

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
