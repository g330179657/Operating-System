package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.io.EOFException;
import java.io.*;
import java.util.*;

public class Pipes extends OpenFile{
    private static final int pageSize = Processor.pageSize;
    private byte[] buffer = new byte[pageSize];
    public UserProcess reader = null;
    public UserProcess writer = null;
    private Lock lock = new Lock();
    private Condition readCond = new Condition(lock);
    private Condition writeCond = new Condition(lock);
    public OpenFile op = null;

    public Pipes(OpenFile op){
        this.op = op;
    }

    public Pipes(FileSystem fileSystem, String name) {
		super(fileSystem, name);
	}
    public Pipes(){
        super();
    }
    protected int size = 0;
    public int read(byte[] buf, int offset, int length) {
        lock.acquire();
        size = op.read(0, buffer, 0, pageSize);
        System.out.println(size);
        /*while(size == 0){
            readCond.sleep();
        }*/
        int n = (size < pageSize-offset) ? size : pageSize-offset;
        n = Math.min(n, length);
        for(int i = 0; i < n; i++){
            buf[offset+i] = buffer[i];
        }
        int amount = n;
        int i = 0;
        for(int j = n; j < size; j++){
            buffer[i] = buffer[j];
            i++;
        }
        size = i;
        writeCond.wake();
        op = ThreadedKernel.fileSystem.open(op.getName(), true);
        op.write(0, buffer, 0, size);
        lock.release();
        return amount;
	}
    

    public int write(byte[] buf, int offset, int length) {
        lock.acquire();
        size = op.read(0, buffer, 0, pageSize);
        while(size == pageSize){
            writeCond.sleep();
        }
        int n = (pageSize-size < pageSize-offset) ? pageSize-size : pageSize-offset;
        n = Math.min(n, length);
        for(int i = 0; i < n; i++){
            buffer[size+i] = buf[i];
        }
        int amount = n;
        size += n;
        lock.release();
        int writebyte = op.write(0, buffer, 0, size);
        System.out.println(writebyte);
        return amount;
	}

    public String getName() {
		return "/pipe/"+op.getName();
	}

}