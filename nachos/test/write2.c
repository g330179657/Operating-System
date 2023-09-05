/*
 * write2.c
 *
 * Write a string to file, one byte at a time.  Does not require any
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
    int fd = creat(filename);
    while (*str) {
        int r = write (fd, str, 1);
        if (r != 1) {
            printf ("failed to write character (r = %d)\n", r);
            exit (-1);
        }
        str++;
    }

    return 0;
}