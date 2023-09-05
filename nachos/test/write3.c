/*
 * write3.c
 *
 * Write a string to file, multiple bytes at a time.  Does not require any
 * of the other system calls to be implemented.
 *
 * Geoff Voelker
 * 11/9/15
 */

#include "syscall.h"

int
main (int argc, char *argv[])
{
    char *str = "\nroses are red\nviolets are blue\nI love Nachos\nand so do you\n\n";
    
    char *filename = "openfile1";
    int fd1 = creat(filename);
    int fd2 = creat(filename);
    
    int r = write (fd1, str, 10);
    printf("written bytes: %d\n", r);
    str += 10;
    
    
    int y = write(fd1, str, 10);
    printf("written bytes: %d\n", y);
    str += 10;
    

    return 0;
}