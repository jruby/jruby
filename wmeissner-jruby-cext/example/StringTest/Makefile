
SHELL = /bin/sh

#### Start of system configuration section. ####

srcdir = .
topdir = /Users/wayne/src/jruby/lib/native
hdrdir = $(topdir)
VPATH = $(srcdir):$(topdir):$(hdrdir)
prefix = $(DESTDIR)
libdir = $(DESTDIR)/Users/wayne/src/jruby/lib
rubylibdir = $(DESTDIR)/Users/wayne/src/jruby/lib/ruby/1.8
sitedir = $(DESTDIR)/Users/wayne/src/jruby/lib/ruby/site_ruby
sitelibdir = $(DESTDIR)/Users/wayne/src/jruby/lib/ruby/site_ruby/1.8
sitearchdir = $(DESTDIR)/Users/wayne/src/jruby/lib/ruby/site_ruby/1.8/java
archdir = $(DESTDIR)/Users/wayne/src/jruby/lib/ruby/1.8/java
datadir = $(DESTDIR)/Users/wayne/src/jruby/share
mandir = $(DESTDIR)/Users/wayne/src/jruby/man
sysconfdir = $(DESTDIR)/Users/wayne/src/jruby/etc
localstatedir = $(DESTDIR)/Users/wayne/src/jruby/var

CC = cc 
LIBRUBY = 
LIBRUBY_A = 
LIBRUBYARG_SHARED = 
LIBRUBYARG_STATIC = 

RUBY_EXTCONF_H = 
CFLAGS   =   -DTARGET_RT_MAC_CFM=0   -arch i386 -arch ppc -arch x86_64 
INCFLAGS = -I. -I$(topdir) -I$(hdrdir) -I$(srcdir)
DEFS     = 
CPPFLAGS =  
CXXFLAGS = $(CFLAGS) 
ldflags  =  -bundle -framework JavaVM -Wl,-syslibroot,$(SDKROOT) -mmacosx-version-min=10.4 -undefined dynamic_lookup 
dldflags = 
archflag =  -arch i386 -arch ppc -arch x86_64 
DLDFLAGS = $(ldflags) $(dldflags) $(archflag)
LDSHARED = cc -dynamic -bundle -undefined suppress -flat_namespace 
AR = 
EXEEXT = 

RUBY_INSTALL_NAME = 
RUBY_SO_NAME = 
arch = java
sitearch = java
ruby_version = 1.8
ruby = /Users/wayne/src/jruby/bin/jruby
RUBY = $(ruby)
RM = rm -f
MAKEDIRS = mkdir -p 
INSTALL = install -c 
INSTALL_PROG = $(INSTALL) -m 0755
INSTALL_DATA = $(INSTALL) -m 0644
COPY = cp 

#### End of system configuration section. ####

preload = 

libpath = . $(libdir)
LIBPATH =  -L"." -L"$(libdir)"
DEFFILE = 

CLEANFILES = mkmf.log
DISTCLEANFILES = 

extout = 
extout_prefix = 
target_prefix = 
LOCAL_LIBS = 
LIBS =    
SRCS = string.c
OBJS = string.o
TARGET = string
DLLIB = $(TARGET).bundle
EXTSTATIC = 
STATIC_LIB = 

BINDIR        = $(bindir)
RUBYCOMMONDIR = $(sitedir)$(target_prefix)
RUBYLIBDIR    = $(sitelibdir)$(target_prefix)
RUBYARCHDIR   = $(sitearchdir)$(target_prefix)

TARGET_SO     = $(DLLIB)
CLEANLIBS     = $(TARGET).bundle $(TARGET).il? $(TARGET).tds $(TARGET).map
CLEANOBJS     = *.o *.a *.s[ol] *.pdb *.exp *.bak

all:    $(DLLIB)
static: $(STATIC_LIB)
.PHONY: all install static install-so install-rb
.PHONY: clean clean-so clean-rb

clean:
		@-$(RM) $(CLEANLIBS) $(CLEANOBJS) $(CLEANFILES)

distclean:	clean
		@-$(RM) Makefile $(RUBY_EXTCONF_H) conftest.* mkmf.log
		@-$(RM) core ruby$(EXEEXT) *~ $(DISTCLEANFILES)

realclean:	distclean
install: install-so install-rb

install-so: $(RUBYARCHDIR)
install-so: $(RUBYARCHDIR)/$(DLLIB)
$(RUBYARCHDIR)/$(DLLIB): $(DLLIB)
	$(INSTALL_PROG) $(DLLIB) $(RUBYARCHDIR)
install-rb: pre-install-rb install-rb-default
install-rb-default: pre-install-rb-default
pre-install-rb: Makefile
pre-install-rb-default: Makefile
$(RUBYARCHDIR):
	$(MAKEDIRS) $@

site-install: site-install-so site-install-rb
site-install-so: install-so
site-install-rb: install-rb

.SUFFIXES: .c .m .cc .cxx .cpp .C .o

.cc.o:
	$(CXX) $(INCFLAGS) $(CPPFLAGS) $(CXXFLAGS) -c $<

.cxx.o:
	$(CXX) $(INCFLAGS) $(CPPFLAGS) $(CXXFLAGS) -c $<

.cpp.o:
	$(CXX) $(INCFLAGS) $(CPPFLAGS) $(CXXFLAGS) -c $<

.C.o:
	$(CXX) $(INCFLAGS) $(CPPFLAGS) $(CXXFLAGS) -c $<

.c.o:
	$(CC) $(INCFLAGS) $(CPPFLAGS) $(CFLAGS) -c $<

$(DLLIB): $(OBJS) Makefile
	@-$(RM) $@
	$(LDSHARED) -o $@ $(OBJS) $(LIBPATH) $(DLDFLAGS) $(LOCAL_LIBS) $(LIBS)



$(OBJS): ruby.h defines.h
