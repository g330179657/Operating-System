/*
 * read3.c
 *
 * read chars from stdin
 *
 */

#include "syscall.h"

int main (int argc, char *argv[])
{
    int fd = 0;
    char buffer[81];

    int readByte = read(0, buffer, 5);
    printf("read number of byte: %d \n", readByte);
    printf(buffer);
    printf("\n");

    return 0;
}