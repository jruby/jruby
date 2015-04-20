p Truffle::CExt.supported?

Truffle::CExt.inline %{
  #include <stdio.h>
}, %{
  printf("FOO was defined to be %d\\n", FOO);
}, %w(-DFOO=14)
