#include <string.h>
#include <stdio.h>

#include <openssl/md5.h>

static char *hello = "hello";

int main(int argc, char **argv) {
  unsigned char digest[MD5_DIGEST_LENGTH];

  MD5((unsigned char *)hello, strlen(hello), digest);

  for (int n = 0; n < MD5_DIGEST_LENGTH; n++) {
    printf("%02x", digest[n]);
  }

  printf("\n");

  return 0;
}
