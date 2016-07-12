#include <stdio.h>

#include <openssl/md5.h>

static char *hello = "hello";

int main(int argc, char **argv) {
    unsigned char hashed[MD5_DIGEST_LENGTH];

    MD5(hashed, sizeof(hello), (unsigned char *)hello);

    for (int n = 0; n < MD5_DIGEST_LENGTH; n++) {
        printf("%02x", hashed[n]);
    }

    printf("\n");

    return 0;
}
