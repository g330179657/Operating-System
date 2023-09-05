/*
 * creat2.c
 *
 * create two existing files
 *
 */

#include "syscall.h"

int main (int argc, char *argv[])
{
    char *filename1 = "openfile1";
    int fd1 = creat(filename1);
    printf("fild descripter: %d  file name: %s\n", fd1, filename1);
    char *filename2 = "openfile2";
    int fd2 = creat(filename2);
    printf("fild descripter: %d  file name: %s\n", fd2, filename2);

    return 0;
}