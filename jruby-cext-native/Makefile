# -*- makefile -*-

JRUBY_DIR = ../../jruby
JRUBY_JAR = $(JRUBY_DIR)/lib/jruby.jar
CEXT_JAVA = ../jruby-cext-java
CEXT_JAR = $(CEXT_JAVA)/dist/jruby-cext-java.jar

BUILD_OS := $(strip $(shell uname -s | tr '[:upper:]' '[:lower:]'))
OS ?= $(BUILD_OS)
ifeq ($(OS),sunos)
  OS = solaris
endif

# Default value of $OS on Windows is Windows_NT
ifeq ($(OS), Windows_NT)
    # that's how we detect x64...
    ifneq ($(findstring 64, $(BUILD_OS)),)
      OS = win64
    else
      OS = win32
    endif
endif

ifneq ($(findstring cygwin, $(BUILD_OS)),)
  # cygwin is always x32
  OS = win32
endif

LIBCEXT = $(BUILD_DIR)/$(PREFIX)$(LIBNAME).$(JNIEXT)

CPU ?= $(shell uname -m | sed -e 's/i[345678]86/i386/')
MODEL = 32 # Default to 32bit compiles
PLATFORM = $(CPU)-$(OS)

JDK_HOME=$(shell if [ -d "$(JAVA_HOME)"/include ];then echo "$(JAVA_HOME)"; else echo "$(JAVA_HOME)"/..; fi)
# Set defaults to unix (linux/solaris/bsd)
PREFIX = lib
JNIEXT = so

export MACOSX_DEPLOYMENT_TARGET=10.4

CCACHE := $(strip $(realpath $(shell which ccache 2> /dev/null)))
SRC_DIR ?= $(shell pwd)/src
JNI_DIR ?= $(shell pwd)/src
BUILD_DIR ?= $(shell pwd)/build

SRCS = $(wildcard $(SRC_DIR)/*.c)
OBJS = $(patsubst %.c, $(BUILD_DIR)/%.o, $(notdir $(SRCS)))

CXXOBJS += $(patsubst %.cpp, $(BUILD_DIR)/%.o, $(notdir $(wildcard $(SRC_DIR)/*.cpp)))

vpath %.h $(SRC_DIR)

LIBNAME = jruby-cext

#
# Compiler/linker flags from:
#   http://weblogs.java.net/blog/kellyohair/archive/2006/01/compilation_of_1.html
JFLAGS = -fno-omit-frame-pointer -fno-strict-aliasing -DNDEBUG
OFLAGS = -O2 $(JFLAGS)

# MacOS headers aren't completely warning free, so turn them off
ifneq ($(OS),darwin)
  WFLAGS = -W -Werror -Wall -Wno-unused -Wno-parentheses -Wundef
endif

PICFLAGS = -fPIC
SOFLAGS = # Filled in for each OS specifically
FFI_MMAP_EXEC = -DFFI_MMAP_EXEC_WRIT

STRIP = strip -S


JDK_INCLUDES = -I"$(JDK_HOME)/include" -I"$(JDK_HOME)/include/$(OS)"
IFLAGS = -I"$(BUILD_DIR)" -I"$(BUILD_DIR)"/jni -I$(SRC_DIR) -I"$(SRC_DIR)" -I"$(BUILD_DIR)"/include
CFLAGS = $(OFLAGS) $(WFLAGS) $(IFLAGS) $(PICFLAGS) $(JDK_INCLUDES)
CFLAGS += -D_REENTRANT -D_LARGEFILE64_SOURCE -D_GNU_SOURCE
CXXFLAGS = $(CFLAGS)

ifeq ($(OS), win64)
  override CPU = x86_64
  JDK_INCLUDES=-I$(JNI_DIR)/win32/include -I$(JNI_DIR)/win32/include/win32
  CC = x86_64-w64-mingw32-gcc -m64
  PICFLAGS =
  ifneq ($(findstring cygwin, $(BUILD_OS)),)
    CC += -mno-cygwin
    LDFLAGS += -mno-cygwin
  endif
  CFLAGS += -mwin32 -D_JNI_IMPLEMENTATION_
  LDFLAGS += -Wl,--add-stdcall-alias
  PICFLAGS=
  SOFLAGS += -shared -mimpure-text -static-libgcc
  PREFIX =
  JNIEXT=dll
  CXX = x86_64-w64-mingw32-g++ -m64
  AR = x86_64-w64-mingw32-ar
  LD = x86_64-w64-mingw32-ld
  STRIP = x86_64-w64-mingw32-strip --strip-debug
  CONFIGURE_BUILD = x86_64-w64-mingw32
endif

ifeq ($(OS),cross-mingw32)
  override OS = win32
  override CPU = i386
  JDK_INCLUDES=-I$(JNI_DIR)/win32/include -I$(JNI_DIR)/win32/include/win32
  CC = i386-mingw32-gcc
  CXX = i386-mingw32-g++
  LD = i386-mingw32-ld
  STRIP = i386-mingw32-strip --strip-debug
  CONFIGURE_HOST = i386-mingw32
endif

ifneq ($(findstring cygwin,$(BUILD_OS)),)
  OS = win32
  JAVA_HOME := $(shell cygpath -u $(JAVA_HOME))
endif

ifeq ($(OS), win32)
  ifneq ($(findstring cygwin, $(BUILD_OS)),)
    CC += -mno-cygwin
    LDFLAGS += -mno-cygwin
  endif
  CFLAGS += -mwin32 -D_JNI_IMPLEMENTATION_
  LDFLAGS += -Wl,--add-stdcall-alias
  PICFLAGS=
  SOFLAGS += -shared -mimpure-text -static-libgcc
  PREFIX =
  JNIEXT=dll
endif

ifeq ($(OS), darwin)
  MACSDK = /Developer/SDKs/MacOSX10.5.sdk
  JDK_INCLUDES = -I$(MACSDK)/System/Library/Frameworks/JavaVM.framework/Headers
  PLATFORM = darwin
  ARCHES = ppc
  ifneq ($(findstring $(CPU), i386 x86_64),)
    ARCHES= i386 x86_64
  endif
  
  CFLAGS += -isysroot $(MACSDK) -DTARGET_RT_MAC_CFM=0
  CFLAGS += $(foreach arch, $(ARCHES),-arch $(arch))
  LDFLAGS = $(foreach arch, $(ARCHES),-arch $(arch)) -dynamiclib -framework JavaVM \
	-Wl,-syslibroot,$(MACSDK) -mmacosx-version-min=10.5
  JNIEXT = jnilib
  #CFLAGS += -I$(MACSDK)/System/Library/Frameworks/Kernel.framework/Versions/A/Headers
  PICFLAGS =
  SOFLAGS =
endif

ifeq ($(OS), linux)
  SOFLAGS = -shared -mimpure-text -static-libgcc -Wl,-soname,$(@F) -Wl,-O1
  CFLAGS += -pthread
endif

ifeq ($(OS), solaris)
  CC = gcc
  CFLAGS += -D__EXTENSIONS__ -std=c99
  LD = /usr/ccs/bin/ld
  SOFLAGS = -shared -static-libgcc -mimpure-text
  LIBS += -ldl
endif

ifeq ($(OS), aix)
  SOFLAGS = -shared -static-libgcc
  CFLAGS += -pthread
  LDFLAGS += -pthread
  JNIEXT = a
  STRIP = strip
endif

ifneq ($(findstring bsd, $(OS)),)
  SOFLAGS = -shared -static-libgcc
  CFLAGS += -pthread
  LDFLAGS += -pthread
endif

ifeq ($(CPU), sparcv9)
  MODEL = 64
endif

ifeq ($(CPU), i386)
  MODEL = 32
  ifeq ($(findstring $(OS), openbsd darwin mingw32 win32),)
    CFLAGS += -march=i586 -mtune=generic
  endif
endif

ifeq ($(CPU), amd64)
  MODEL = 64
endif

ifeq ($(CPU), x86_64)
  MODEL = 64
endif

ifeq ($(CPU), s390x)
  MODEL = 64
endif

# On platforms (linux, solaris) that support both 32bit and 64bit, force building for one or the other
ifneq ($(strip $(findstring $(OS), linux solaris)),)
  # Change the CC/LD instead of CFLAGS/LDFLAGS, incase other things in the flags
  # makes the libffi build choke
  CC += -m$(MODEL)
  CXX += -m$(MODEL)
  LD += -m$(MODEL)
endif


JAVA_HDRS = $(patsubst %.h, $(BUILD_DIR)/%.h, org_jruby_cext_Native.h org_jruby_cext_GC.h  org_jruby_runtime_ClassIndex.h)

all:	$(LIBCEXT)

compile:	all

debug:
	@echo OS="$(OS)"
	@echo BUILD_OS="$(BUILD_OS)"
	@echo BUILD_DIR="$(BUILD_DIR)"
	@echo CPU="$(CPU)"
	@echo JAVA_HOME="$(JAVA_HOME)"
	@echo JDK_HOME="$(JDK_HOME)"
	@echo JAVA_HDRS="$(JAVA_HDRS)"
	@echo CEXT_JAVA="$(CEXT_JAVA)"
	@echo "PLATFORM=$(PLATFORM)"
	@echo "OBJS=$(OBJS)"

$(LIBCEXT):  $(OBJS) $(CXXOBJS)
	$(CXX) -o $@ $(LDFLAGS) $(SOFLAGS) $(OBJS) $(CXXOBJS) $(LIBS)
	

$(BUILD_DIR)/%.o: $(SRC_DIR)/%.c $(wildcard $(SRC_DIR)/*.h) $(JAVA_HDRS)
	@mkdir -p $(@D)
	$(CCACHE) $(CC) $(CFLAGS) -c $< -o $@

$(BUILD_DIR)/%.o: $(SRC_DIR)/%.cpp  $(wildcard $(SRC_DIR)/*.h) $(JAVA_HDRS)
	@mkdir -p $(@D)
	$(CCACHE) $(CXX) $(CXXFLAGS) -c $< -o $@


.SECONDARY: $(JAVA_HDRS)

$(JAVA_HDRS):	$(JRUBY_JAR) $(CEXT_JAR)
	@mkdir -p $(@D)
	javah -classpath $(CEXT_JAR):$(JRUBY_JAR) -d $(@D) $(subst _,., $(basename $(@F)))


$(CEXT_JAR):	$(wildcard $(CEXT_JAVA)/src/org/jruby/cext/*.java)
	cd $(CEXT_JAVA) && ant jar

.PHONY: clean
clean:
	$(RM) -rf build

