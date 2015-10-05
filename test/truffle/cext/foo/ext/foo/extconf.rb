require 'mkmf'
$CFLAGS << ' -Wall'
create_makefile('foo/foo')
