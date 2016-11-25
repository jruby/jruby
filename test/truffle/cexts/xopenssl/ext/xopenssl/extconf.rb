require 'mkmf'
$CFLAGS += " -I#{ENV['OPENSSL_HOME']}/include"
create_makefile('xopenssl')
