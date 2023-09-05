/*
 * creat3.c
 *
 * create two existing files
 *
 */

#include "syscall.h"

int main (int argc, char *argv[])
{
    for(int i = 0; i < 16; i++){
        char *filename = "openfile1";
        int fd = creat(filename);
        printf("fild descripter: %d  file name: %s\n", fd, filename);
    }

    return 0;
}