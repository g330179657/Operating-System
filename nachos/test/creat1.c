/*
 * creat1.c
 *
 * create one unexisted file
 *
 */

#include "syscall.h"

int main (int argc, char *argv[])
{
    char *filename = "openfile3";
    int fd = creat(filename);
    printf("fild descripter: %d  file name: %s\n", fd, filename);

    return 0;
}