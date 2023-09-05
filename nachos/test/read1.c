/*
 * read1.c
 *
 * read chars from openfile1 one times
 *
 */

#include "syscall.h"

int main (int argc, char *argv[])
{
    char *filename = "openfile1";
    int fd = open(filename);
    char buffer[81];

    
    int readByte = read(fd, buffer, 5);
    printf("read number of byte: %d \n", readByte);
    printf(buffer);
    printf("\n");
    return 0;
}