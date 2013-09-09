# -*- makefile -*-

JRUBY_BASEDIR = $(shell pwd)/../../
JRUBY_CLASSES = $(JRUBY_BASEDIR)/core/target/classes

GCC_VERSION := $(strip $(shell gcc --version))
ifneq ($(strip $(findstring 4.,$(GCC_VERSION))),)
  ifeq ($(strip $(findstring 4.0,$(GCC_VERSION))),)
    ifeq ($(strip $(findstring 4.1,$(GCC_VERSION))),)
      SUPPORTED_GCC := $(GCC_VERSION)
    endif
  endif
endif

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

LIBCEXT = $(JRUBY_NATIVE_DIR)/$(PLATFORM)/$(PREFIX)$(LIBNAME).$(JNIEXT)

CPU ?= $(shell uname -m | sed -e 's/i[345678]86/i386/' | sed -e 's/amd64/x86_64/')
MODEL = 32 # Default to 32bit compiles

# Try to determine JAVA_HOME if not set
ifndef JAVA_HOME
  JAVAC := $(strip $(realpath $(shell which javac)))
  ifdef JAVAC
    JAVA_HOME := $(dir $(dir $(JAVAC)))
  endif
endif
JDK_HOME=$(shell if [ -d "$(JAVA_HOME)"/include ];then echo "$(JAVA_HOME)"; else echo "$(JAVA_HOME)"/..; fi)
# Set defaults to unix (linux/solaris/bsd)
PREFIX = lib
JNIEXT = so

export MACOSX_DEPLOYMENT_TARGET=10.4

CCACHE := $(strip $(realpath $(shell which ccache 2> /dev/null)))
SRC_DIR ?= $(shell pwd)
JNI_DIR ?= $(SRC_DIR)
JRUBY_NATIVE_DIR ?= $(JRUBY_BASEDIR)/lib/jni/
BUILD_DIR ?= $(JRUBY_BASEDIR)/build
HDR_DIR ?= $(JRUBY_NATIVE_DIR)/include
RUBY_HEADERS = $(patsubst %.h, $(HDR_DIR)/ruby/%.h, $(notdir $(wildcard $(SRC_DIR)/include/ruby/*.h)))
RUBY_HEADERS += $(patsubst %.h, $(HDR_DIR)/%.h, $(notdir $(wildcard $(SRC_DIR)/include/*.h)))

SRCS = $(wildcard $(SRC_DIR)/*.c)
OBJS = $(patsubst %.c, $(BUILD_DIR)/%.o, $(notdir $(SRCS)))

CXXOBJS += $(patsubst %.cpp, $(BUILD_DIR)/%.o, $(notdir $(wildcard $(SRC_DIR)/*.cpp)))

vpath %.h $(SRC_DIR)

LIBNAME = jruby-cext

#
# Compiler/linker flags from:
#   http://weblogs.java.net/blog/kellyohair/archive/2006/01/compilation_of_1.html
JFLAGS = -fno-omit-frame-pointer -fno-strict-aliasing
DBG_FLAGS = -DNDEBUG
OFLAGS = $(DBG_FLAGS) $(JFLAGS)

WFLAGS = -W -Wall -Wno-unused -Wno-unused-parameter -Wno-parentheses -Werror
# MacOS headers have some undefined macros, so don't warn on these on darwin
ifneq ($(OS),darwin)
  WFLAGS += -Wundef
endif

PICFLAGS = -fPIC
SOFLAGS = # Filled in for each OS specifically

STRIP = strip -S

# Only set JDK includes if not previously set.
ifndef JDK_INCLUDES
	JDK_INCLUDES = -I"$(JDK_HOME)/include" -I"$(JDK_HOME)/include/$(OS)"
endif
IFLAGS = -I"$(BUILD_DIR)" -I"$(SRC_DIR)" -I"$(BUILD_DIR)"/jni \
  -I"$(SRC_DIR)"/include -I"$(SRC_DIR)"/include/ruby
CFLAGS = $(OFLAGS) $(WFLAGS) $(IFLAGS) $(PICFLAGS) $(JDK_INCLUDES) $(ARCHFLAGS)
CFLAGS += -D_REENTRANT -D_LARGEFILE64_SOURCE -D_GNU_SOURCE -ggdb -O0
CXXFLAGS = $(CFLAGS)

ifeq ($(OS), win64)
  PLATFORM = x86_64-Windows
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
  SOFLAGS += -shared -static-libgcc
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
  PLATFORM = i386-Windows
  ifneq ($(findstring cygwin, $(BUILD_OS)),)
    CC += -mno-cygwin
    LDFLAGS += -mno-cygwin
  endif
  CFLAGS += -march=native -mtune=native -D_JNI_IMPLEMENTATION_ -DRUBY_DLLSPEC="__declspec(dllexport)"
  LDFLAGS += -Wl,--add-stdcall-alias
  PICFLAGS=
  SOFLAGS += -shared -Wl,--enable-auto-image-base,--enable-auto-import
  PREFIX =
  JNIEXT=dll
  LIBS += -lws2_32
endif

ifeq ($(OS), darwin)
  PLATFORM = Darwin
  # First try to locate a legacy Xcode 3 install, so a universal binary (ppc + intel) can be built
  ifneq ($(realpath /Xcode3/usr/bin/gcc),)
    XCODE=/Xcode3
  else
    XCODE=$(shell xcode-select -print-path)
  endif

  ifneq ($(realpath $(wildcard $(XCODE)/usr/bin/gcc)),)
    CC = $(XCODE)/usr/bin/gcc
    CXX = $(XCODE)/usr/bin/g++
  else
    CC = cc
    CXX = c++
  endif

  ARCHES =
  ifneq ($(findstring $(CPU), ppc powerpc),)
    ARCHES += ppc
  endif
  ifneq ($(findstring $(CPU), i386 x86_64),)
    ARCHES += i386 x86_64
  endif

  UNIVERSAL_SDK = $(firstword $(strip $(realpath $(XCODE)/SDKs/MacOSX10.5.sdk)))
  ifneq ($(UNIVERSAL_SDK),)
    MACSDK = $(UNIVERSAL_SDK)
    ifeq ($(findstring ppc, $(ARCHES)),)
      ARCHES += ppc
    endif
  else
    MACSDK = $(firstword $(strip $(wildcard $(XCODE)/SDKs/MacOSX10.*.sdk)) $(strip $(wildcard $(XCODE)/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.*.sdk)))
  endif

  JDK_INCLUDES = -I$(MACSDK)/System/Library/Frameworks/JavaVM.framework/Headers
  IFLAGS += -isysroot $(MACSDK) -I$(MACSDK)/usr/include
  CFLAGS += -DTARGET_RT_MAC_CFM=0 
  ARCHFLAGS = $(foreach arch, $(ARCHES),-arch $(arch))
  LDFLAGS = $(foreach arch, $(ARCHES),-arch $(arch)) -dynamiclib -framework JavaVM \
	-Wl,-syslibroot,$(MACSDK) -mmacosx-version-min=10.5
  JNIEXT = jnilib
  PICFLAGS =
  SOFLAGS =
endif

ifeq ($(OS), linux)
  PLATFORM = $(CPU)-Linux
  SOFLAGS = -shared -static-libgcc -Wl,-soname,$(@F) -Wl,-O1
  CFLAGS += -pthread
endif

ifeq ($(OS), solaris)
  ifeq ($(CPU), i86pc)
    override CPU = i386
  endif
  PLATFORM = $(CPU)-SunOS
  CC = gcc
  CFLAGS += -D__EXTENSIONS__ 
  LD = /usr/ccs/bin/ld
  SOFLAGS = -shared -static-libgcc
  LIBS += -ldl
endif

ifeq ($(OS), aix)
  PLATFORM = $(CPU)-AIX
  SOFLAGS = -shared -static-libgcc
  CFLAGS += -pthread
  LDFLAGS += -pthread
  JNIEXT = a
  STRIP = strip
endif

ifneq ($(findstring bsd, $(OS)),)
  ifneq ($(findstring openbsd, $(OS)),)
    PLATFORM = $(CPU)-OpenBSD
  else
    PLATFORM = $(CPU)-FreeBSD
  endif
  SOFLAGS = -shared -static-libgcc
  CFLAGS += -pthread
  LDFLAGS += -pthread
endif

ifneq ($(findstring sun4, $(CPU)),)
  CPU=sparc
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

JAVA_HDRS = $(patsubst %.h, $(BUILD_DIR)/%.h, org_jruby_cext_JRuby.h org_jruby_cext_Native.h org_jruby_cext_GC.h  org_jruby_runtime_ClassIndex.h)

all:
ifndef SUPPORTED_GCC
	@echo "Only GCCs above or equal to 4.2 are supported for JRuby C extensions"
	@echo "$(GCC_VERSION)"
else
	@$(MAKE) compile
endif


compile:	$(LIBCEXT)

debug : DBG_FLAGS=-DDEBUG -g -O0

debug: compile
	@echo OS="$(OS)"
	@echo BUILD_OS="$(BUILD_OS)"
	@echo BUILD_DIR="$(BUILD_DIR)"
	@echo CPU="$(CPU)"
	@echo JAVA_HOME="$(JAVA_HOME)"
	@echo JDK_HOME="$(JDK_HOME)"
	@echo JDK_INCLUDES="$(JDK_INCLUDES)"
	@echo JAVA_HDRS="$(JAVA_HDRS)"
	@echo JAVAC="$(JAVAC)"
	@echo CEXT_JAVA="$(CEXT_JAVA)"
	@echo "PLATFORM=$(PLATFORM)"
	@echo "OBJS=$(OBJS)"

$(LIBCEXT):  $(OBJS) $(CXXOBJS) $(RUBY_HEADERS)
	@mkdir -p $(BUILD_DIR)
	$(CXX) -o $@ $(LDFLAGS) $(SOFLAGS) $(OBJS) $(CXXOBJS) $(LIBS)

$(BUILD_DIR)/%.o: $(SRC_DIR)/%.c $(wildcard $(SRC_DIR)/*.h) $(JAVA_HDRS)
	$(CCACHE) $(patsubst %ccache,,$(CC)) $(CFLAGS) -c $< -o $@

$(BUILD_DIR)/%.o: $(SRC_DIR)/%.cpp  $(wildcard $(SRC_DIR)/*.h) $(JAVA_HDRS)
	@mkdir -p $(@D)
	$(CCACHE) $(patsubst %ccache,,$(CXX)) $(CXXFLAGS) -c $< -o $@

$(RUBY_HEADERS): $(wildcard $(SRC_DIR)/include/ruby/*.h) $(SRC_DIR)/include/ruby.h
	@mkdir -p $(HDR_DIR)/ruby
	@cp $(wildcard $(SRC_DIR)/include/ruby/*.h) $(HDR_DIR)/ruby/
	@cp $(wildcard $(SRC_DIR)/include/*.h) $(HDR_DIR)/

.SECONDARY: $(JAVA_HDRS)

$(JAVA_HDRS):
	@mkdir -p $(@D)
	javah -classpath $(JRUBY_CLASSES) -d $(@D) $(subst _,., $(basename $(@F)))

.PHONY: clean
clean: dist-clean
	$(RM) $(LIBCEXT)
	$(RM) $(wildcard config.*)
	$(RM) $(wildcard configure)
	$(RM) -R autom4te.cache
	$(RM) $(RUBY_HEADERS)


dist-clean:
	$(RM) $(OBJS) $(JAVA_HDRS) $(CXXOBJS)
