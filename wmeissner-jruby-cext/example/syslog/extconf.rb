# $RoughId: extconf.rb,v 1.3 2001/11/24 17:49:26 knu Exp $
# $Id: extconf.rb 11708 2007-02-12 23:01:19Z shyouhei $

require 'mkmf'

have_header("syslog.h") &&
  have_func("openlog") &&
  have_func("setlogmask") &&
  create_makefile("syslog")

