/*
 * exec1.c
 *
 * Simple program for testing exec.  It does not pass any arguments to
 * the child.
 */

#include "syscall.h"

int
main (int argc, char *argv[])
{
    char *prog = "exit1.coff";
    int pid;
    printf("before exec\n");
    pid = exec (prog, 0, 0);
    printf("after exec\n");
    int status = 0;
    printf("child pid: %d\n", pid);
    if (pid < 0) {
	exit (-1);
    //printf("called exception");
    }
    exit (2);
}