/*
 * open1.c
 *
 * open a file
 *
 */

#include "syscall.h"

int main (int argc, char *argv[])
{
    char *filename = "openfile1";
    int fd = open(filename);
    printf("file descripter is %d\n", fd);
    return 0;
}