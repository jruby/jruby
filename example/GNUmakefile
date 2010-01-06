SUBDIRS += ArrayTest DefineClass HelloWorld MyTest RaiseException
SUBDIRS += etc fcntl http11 syslog

all clean:
	for i in $(SUBDIRS);do $(MAKE) -C $$i $@;done
