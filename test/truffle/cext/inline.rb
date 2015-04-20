p Truffle::CExt.supported?

Truffle::CExt.inline %{
  #include <stdio.h>
}, %{
  printf("Hello, World!\\n");
}
