package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.io.EOFException;
import java.io.*;
import java.util.*;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	public int pid;
	public static int pidCounter = 0;
	public int ppid;
	public UserProcess parentProcess;
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		/*
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i = 0; i < numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
		*/
		fileTable = new OpenFile[MAXTABLESIZE];
		fileTable[0] = UserKernel.console.openForReading();
		fileTable[1] = UserKernel.console.openForWriting();
		for(int i = 2; i < 16; i++){
			fileTable[i] = null;
		}
		this.pid = pidCounter;
		this.ppid = -1;
		this.parentProcess = null;
		pidCounter++;
		countLock.acquire();
		processCount++;
		countLock.release();
		System.out.println("Create process pid: " + pid);
	}

	public int getPid(){
		return this.pid;
	}


	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
	        String name = Machine.getProcessClassName ();

		// If Lib.constructObject is used, it quickly runs out
		// of file descriptors and throws an exception in
		// createClassLoader.  Hack around it by hard-coding
		// creating new processes of the appropriate type.

		if (name.equals ("nachos.userprog.UserProcess")) {
		    return new UserProcess ();
		} else if (name.equals ("nachos.vm.VMProcess")) {
		    return new VMProcess ();
		} else {
		    return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
		}
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;
		//System.out.println("In execute function");
		System.out.println("file name: " + name + " args.length: " + args.length);
		thread = new UThread(this);
		thread.setName(name).fork();
		System.out.println(thread.getName());
		System.out.println("execute successfully");
		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		//System.out.println("I am reading");
		//System.out.println("offser = " + offset + " length = " + length + " data.length = " + data.length);
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);
		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		/*
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;
		
		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(memory, vaddr, data, offset, amount);
		*/

		// task 2
		int amount = 0;
		int remain = length;
		if(vaddr < 0 || vaddr >= memory.length){
			return 0;
		}
		try{
			while(remain > 0){
				int vpn = vaddr / pageSize;
				int voff = vaddr % pageSize;
				if(vpn >= pageTable.length){
					return amount;
				}
				TranslationEntry te = pageTable[vpn];
				int ppn = te.ppn;
				te.used = true;
				int n = (remain < pageSize - voff) ? remain : pageSize-voff;
				//System.out.println("R vpn: " + vpn + " ppn: " + ppn);
				System.arraycopy(memory, ppn*pageSize + voff, data, offset, n);
				vaddr += n;
				offset += n;
				remain -= n;
				amount += n;
			}

			return amount;
		}
		catch(Exception e){
			System.out.println(e.getMessage());
			return amount;
		}
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		//System.out.println("I am writing");
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		/*
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(data, offset, memory, vaddr, amount);
		*/

		// task2
		int amount = 0;
		int remain = length;
		if(vaddr < 0 || vaddr >= memory.length){
			return 0;
		}
		try{
			
			while(remain > 0){
				int vpn = vaddr / pageSize;
				int voff = vaddr % pageSize;
				if(vpn >= pageTable.length){
					return amount;
				}
				TranslationEntry te = pageTable[vpn];
				if(te.readOnly){
					return amount;
				}
				int ppn = te.ppn;
				te.used = true;
				te.dirty = true;
				int n = (remain < pageSize - voff) ? remain : pageSize-voff;
				//System.out.println("W vpn: " + vpn + " ppn: " + ppn);
				System.arraycopy(data, offset, memory, ppn*pageSize + voff, n);
				vaddr += n;
				offset += n;
				remain -= n;
				amount += n;
			}
			return amount;
		}
		catch(Exception e){
			System.out.println(e.getMessage());
			return amount;
		}
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	protected String coffName;
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
		coffName = name;
		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
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

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;
		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}
		System.out.println("return from load");
		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		//int numPhysPages = Machine.processor().getNumPhysPages();
		//pageTable = new TranslationEntry[numPhysPages];
		/*
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}
		*/
		UserKernel.pageLock.acquire();
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPages];
		for(int i = 0; i < numPages; i++){
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
		}
		if (numPages > UserKernel.freePhysPages.size()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			UserKernel.pageLock.release();
			return false;
		}
		
		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			//System.out.println("section: " + s + " section.length = " + section.getLength());
			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");
			
			for (int i = 0; i < section.getLength(); i++) {
				
				int vpn = section.getFirstVPN() + i;
				//System.out.println("	vpn: " + vpn);
				// for now, just assume virtual addresses=physical addresses
				//section.loadPage(i, vpn);

				// task 2
				/*if(UserKernel.freePhysPages.size() == 0){
					return false;
				}*/

				//UserKernel.pageLock.acquire();
				int ppn = UserKernel.freePhysPages.poll();
				//UserKernel.pageLock.release();

				boolean valid = true;
				boolean readOnly = section.isReadOnly();
				boolean used = false;
				boolean dirty = false;
				pageTable[vpn].ppn = ppn;
				pageTable[vpn].readOnly = readOnly;

				section.loadPage(i, ppn);
			}
		}
		for(int j = numPages-9; j < numPages; j++){
			int ppn = UserKernel.freePhysPages.poll();
			pageTable[j].ppn = ppn;
		}

		/*System.out.println("Page Table: ");
		for(int i = 0; i < numPages; i++){
			System.out.println("vpn: " + i + " ppn: " + pageTable[i].ppn);
		}*/
		UserKernel.pageLock.release();
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		for(int i = 0; i < numPages; i++){
			UserKernel.freePhysPages.add(pageTable[i].ppn);
		}
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {
		if(this.pid != 0){
			return -1;
		}

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	/**
	 * Handle the exec() system call.
	 */
	private int handleExec(int vaddr, int argcount, int argvptr){
		System.out.println("exec");
		if(argcount < 0){
			return -1;
		}
		String filename = readVirtualMemoryString(vaddr, MAXLENGTH);

		// check file name
		System.out.println("filename: " + filename);
		if(filename.contains(".")){
			String[] strs = filename.split("\\.");
			if(strs.length < 1) return -1;
			if(!strs[strs.length-1].equals("coff")){
				System.out.println(strs[strs.length-1]);
				return -1;
			}
		}else{
			System.out.println("Invalid file name: " + filename);
			return -1;
		}
		
		// get arguments
		String[] childArgv = new String[argcount];
		for(int i = 0; i < argcount; i++){
			byte[] bytes = new byte[4];
			readVirtualMemory(argvptr, bytes);
			int ptr = Lib.bytesToInt(bytes, 0);
			childArgv[i] = readVirtualMemoryString(ptr, MAXLENGTH);
			argvptr += 4;
			System.out.println("The " + i + "th argument is " + childArgv[i]);
		}

		UserProcess child = newUserProcess();
		child.ppid = this.pid;
		child.parentProcess = this;

		ProcessObj po = new ProcessObj(child, null);
		childProcess.put(child.getPid(), po);
		child.execute(filename, childArgv);
		System.out.println("End of handleExec child pid:" + child.getPid());
		
		return child.getPid();

	}

	/**
	 * Handle the join() system call.
	 */
	private int handleJoin(int pid, int statusVaddr){
		if(!childProcess.containsKey(pid)){
			return -1;
		}
		ProcessObj child = childProcess.get(pid);
		if(child.status != null){
			childProcess.remove(pid);
			int status = child.status;
			byte[] data = Lib.bytesFromInt(status);
			writeVirtualMemory(statusVaddr, data);
			return 1;
		}
		child.up.thread.join();
		childProcess.remove(pid);
		Integer status = child.status;
		if(status == null){
			return 0;
		}
		byte[] data = Lib.bytesFromInt(status);
		writeVirtualMemory(statusVaddr, data);
		return 1;
	}

	class ProcessObj{
		UserProcess up;
		Integer status;

		public ProcessObj(UserProcess up, Integer s){
			this.up = up;
			this.status = s;
		}
	}
	Map<Integer, ProcessObj> childProcess = new HashMap<>();

	/**
	 * Handle the exit() system call.
	 */
	private boolean isNormal = true;
	private int handleExit(int status) {
	    System.out.println("	Inside HandleExit");
		    // Do not remove this call to the autoGrader...
		Machine.autoGrader().finishingCurrentProcess(status);
		// ...and leave it as the top of handleExit so that we
		// can grade your implementation.
		

		for(int i = 0; i < MAXTABLESIZE; i++){
			if(fileTable[i] != null){
				handleClose(i);
			}
		}
		for(Integer cpid : childProcess.keySet()){
			ProcessObj c = childProcess.get(cpid);
			c.up.parentProcess = null;
		}
		if(this.parentProcess != null){
			if(isNormal){
				this.parentProcess.childProcess.get(this.pid).status = status;
			}
			else{
				this.parentProcess.childProcess.get(this.pid).status = null;
			}
		}
		unloadSections();
		coff.close();
		Lib.debug(dbgProcess, "UserProcess.handleExit (" + status + ")");

		countLock.acquire();
		processCount--;
		System.out.println("processCount: " + processCount + " status: " + status);
		if(processCount == 0){
			countLock.release();
			Kernel.kernel.terminate();
		}
		countLock.release();
		KThread.currentThread().finish();
		
		
		return 0;

	}
	public static Lock countLock = new Lock();
	public static int processCount = 0;
	final static int MAXTABLESIZE = 16;
	final static int MAXLENGTH = 256;
	OpenFile[] fileTable;
	/*
	OpenFile[] fileTable = new OpenFile[MAXTABLESIZE];
	fileTable[0] = UserKernel.console.openForReading();
	fileTable[1] = UserKernel.console.openForWriting();
	*/

	/**
	 * Handle the creat() system call.
	 */
	private int handleCreat(int addr){
		String name = readVirtualMemoryString(addr, MAXLENGTH);
		boolean truncate = true;
		try{
			// pipes-----------------------
			String[] strs = name.split("/");
			if(strs.length > 1 && strs[1].equals("pipe")){
				Pipes p = new Pipes(ThreadedKernel.fileSystem.open(strs[2], false));
				if(p.op == null){
					p.op = ThreadedKernel.fileSystem.open(strs[2], true);
					p.writer = this;
					for(int i = 0; i < fileTable.length; i++){
						if(fileTable[i] == null){
							fileTable[i] = p;
							System.out.println("i: " + i);
							return i;
						}
					}
				}
				return -1;
			}
			// pipes^^^^^^^^^^^^^^^^^^^^^^^^

			OpenFile op = ThreadedKernel.fileSystem.open(name, truncate);
			int ret = -1;
			for(int i = 0; i < fileTable.length; i++){
				if(fileTable[i] == null){
					fileTable[i] = op;
					ret = i;
					break;
				}
			}
			return ret;
		}
		catch(Exception e){
			e.printStackTrace();
			return -1;
		}
		

	 }

	 /**
	  * Handle the open() system call.
	  */
	private int handleOpen(int addr){
		String name = readVirtualMemoryString(addr, MAXLENGTH);
		boolean truncate = false;
		try{
			// pipes-------------------------
			String[] strs = name.split("/");
			if(strs.length > 1 && strs[1].equals("pipe")){
				Pipes p = new Pipes(ThreadedKernel.fileSystem.open(strs[2], false));
				if(p.op != null){
					if(p.reader == null){
						p.reader = this;
						for(int i = 0; i < fileTable.length; i++){
							if(fileTable[i] == null){
								fileTable[i] = p;
								return i;
							}
						}
					}
				}
				return -1;
			}
			// pipes^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			OpenFile op = ThreadedKernel.fileSystem.open(name, truncate);
			if(op == null){
				return -1;
			}
			int ret = -1;
			for(int i = 0; i < 16; i++){
				if(fileTable[i] == null){
					fileTable[i] = op;
					ret = i;
					break;
				}
			}
			return ret;
		}
		catch(Exception e){
			return -1;
		}
	}

	byte[] readBuf = new byte[pageSize];
	/**
	 * Handle the read() system call.
	 */
	 private int handleRead(int fd, int bufferaddr, int count){
		if(fd < 0 || fd >= MAXTABLESIZE || fileTable[fd] == null){
			return -1;
		}
		if(count < 0){
			return -1;
		}
		int remain = count;
		//System.out.println("file descriptor = " + fd + " count = " + count + " page size = " + pageSize);
		try{
			int totalByte = 0;
			OpenFile op = fileTable[fd];
			// pipes---------------------------
			String[] strs = op.getName().split("/");
			if(strs.length > 1 && strs[1].equals("pipe")){
				Pipes p = (Pipes)fileTable[fd];
				if(p != null){
					totalByte = 0;
					while(remain > 0){
						int n = (remain < pageSize) ? remain : pageSize;
						int readByte = p.read(readBuf, 0, n);
						int transferByte = writeVirtualMemory(bufferaddr, readBuf, 0, readByte);
						totalByte += transferByte;
						remain -= n;
						bufferaddr += transferByte;
					}
					return totalByte;
				}
				return -1;
			}
			// pipes^^^^^^^^^^^^^^^^^^^^^^^^^^6

			while(remain > 0){
				int n = (remain < pageSize) ? remain : pageSize;
				int readByte = op.read(readBuf, 0, n);
				int transferByte = writeVirtualMemory(bufferaddr, readBuf, 0, readByte);
				totalByte += transferByte;
				remain -= n;
				bufferaddr += transferByte;
			}
			
			return totalByte;
		}
		catch(Exception e){
			return -1;
		}
	 }

	byte[] writeBuf = new byte[pageSize];
	/**
	 * Handle the write() system call.
	 */
	private int handleWrite(int fd, int bufferaddr, int count){
		if(fd < 0 || fd >= MAXTABLESIZE || fileTable[fd] == null){
			return -1;
		}
		if(count < 0){
			return -1;
		}
		int remain = count;
		//System.out.println("file descriptor = " + fd + " count = " + count + " page size = " + pageSize);
		try{
			int totalByte = 0;
			OpenFile op = fileTable[fd];
			// pipes----------------------------
			String[] strs = op.getName().split("/");
			if(strs.length > 1 && strs[1].equals("pipe")){
				Pipes p = (Pipes)fileTable[fd];
				if(p != null){
					totalByte = 0;
					while(remain > 0){
						int n = (remain < pageSize) ? remain : pageSize;
						int transferByte = readVirtualMemory(bufferaddr, writeBuf, 0, n);
						int writeByte = p.write(writeBuf, 0, transferByte);
						totalByte += writeByte;
						remain -= n;
						bufferaddr += transferByte;
					}
					if(totalByte < count){
						return -1;
					}
					return totalByte;
				}
				
				return -1;
			}
			// pipes^^^^^^^^^^^^^^^^^^^^^^^^^^^

			while(remain > 0){
				int n = (remain < pageSize) ? remain : pageSize;
				int transferByte = readVirtualMemory(bufferaddr, writeBuf, 0, n);
				int writeByte = op.write(writeBuf, 0, transferByte);
				totalByte += writeByte;
				remain -= n;
				bufferaddr += transferByte;
			}
			if(totalByte < count){
				return -1;
			}
			return totalByte;

		}
		catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * Handle the close() system call.
	 */
	private int handleClose(int fd){
		if(fd < 0 || fd >= MAXTABLESIZE){
			return -1;
		}
		try{
			OpenFile op = fileTable[fd];
			if(op == null){
				return -1;
			}
			

			// pipes------------------
			String[] strs = op.getName().split("/");
			if(strs.length > 1 && strs[1].equals("pipe")){
				System.out.println("here");
				Pipes p = (Pipes)fileTable[fd];
				System.out.println("writer? " + p == null);
				if(p.writer == this){
					p.writer = null;
				}
				if(p.reader == this){
					p.reader = null;
				}
				if(p.reader == null && p.writer == null){
					ThreadedKernel.fileSystem.remove(p.op.getName());
				}
				return 0;
			}
			//pipes^^^^^^^^^^^^^^^^^^^^^
			fileTable[fd] = null;
			op.close();

			return 0;
		}
		catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * Handel the unlink() system call.
	 */
	private int handleUnlink(int vaddr){
		String name = readVirtualMemoryString(vaddr, MAXLENGTH);
		if(name == null){
			return -1;
		}
		try{
			for(int i = 0; i < MAXTABLESIZE; i++){
				if(fileTable[i] != null && fileTable[i].getName() == name){
					fileTable[i] = null;
					break;
				}
			}
			boolean succ = ThreadedKernel.fileSystem.remove(name);
			return succ ? 0 : -1;
		}
		catch(Exception e){
			return -1;
		}

	}



	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		//System.out.println("here");
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		case syscallExit:
			return handleExit(a0);
		case syscallCreate:
			return handleCreat(a0);
		case syscallOpen:
			return handleOpen(a0);
		case syscallRead:
			return handleRead(a0, a1, a2);
		case syscallWrite:
			return handleWrite(a0, a1, a2);
		case syscallClose:
			return handleClose(a0);
		case syscallUnlink:
			return handleUnlink(a0);
		case syscallExec:
			return handleExec(a0, a1, a2);
		case syscallJoin:
			return handleJoin(a0, a1);
		
		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
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
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			System.out.println("Arrive Exception handler!!!!! pid: " + this.getPid());
			isNormal = false;
			handleExit(-2);
			Lib.assertNotReached("Unexpected exception");
		}
	}


	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	/** The thread that executes the user-level program. */
    protected UThread thread;
    
	private int initialPC, initialSP;

	private int argc, argv;

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';
}
