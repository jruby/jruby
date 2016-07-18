#include <stdio.h>

#include <libxml/xmlstring.h>

int main(int argc, char **argv) {
  printf("%d\n", xmlUTF8Strlen(BAD_CAST "hello π"));
  return 0;
}
