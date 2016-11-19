require 'mkmf'
# $CFLAGS += " -I#{ENV['OPENSSL_INCLUDE']}"
create_makefile('xopenssl')
