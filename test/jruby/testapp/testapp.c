#include <stdio.h>

int main(int argc, char *argv[]) {
    int i;
    // printf("This program is named '%s'.\n", argv[0]);

    if (argc == 1) { // only name
      printf("NO_ARGS\n");
    }

    for (i = 1; i < argc; ++i) {
        printf("%s\n", argv[i]);
    }

    return 0;
}
