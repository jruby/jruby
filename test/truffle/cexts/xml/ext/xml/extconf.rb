require 'mkmf'
$CFLAGS += " -I #{ENV['LIBXML_INCLUDE']}"
$LIBS += " -l #{ENV['LIBXML_LIB']}"
create_makefile('xml')
