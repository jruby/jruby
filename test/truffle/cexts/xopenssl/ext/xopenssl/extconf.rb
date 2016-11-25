require 'mkmf'
$CFLAGS += " -I#{ENV['OPENSSL_INCLUDE']}"
$LDFLAGS += " -l #{ENV['OPENSSL_LIB']}"
create_makefile('xopenssl')
