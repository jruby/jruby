require 'mkmf'
$CFLAGS += " -I#{ENV['LIBXML_INCLUDE']}"
create_makefile('xml')
