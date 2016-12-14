require 'mkmf'
$CFLAGS += " -I #{ENV['OPENSSL_INCLUDE']}"
$LIBS += " -l #{ENV['OPENSSL_LIB']}"
create_makefile('xopenssl')
