require 'mkmf'
$CFLAGS += " -I#{ENV['LIBXML_INCLUDE']}"
$LDFLAGS += " -l #{ENV['LIBXML_LIB']}"
create_makefile('xml')
