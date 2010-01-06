#
# Generated Makefile - do not edit!
#
# Edit the Makefile in the project folder instead (../Makefile). Each target
# has a -pre and a -post target defined where you can add customized code.
#
# This makefile implements configuration specific macros and targets.


# Environment
MKDIR=mkdir
CP=cp
CCADMIN=CCadmin
RANLIB=ranlib
CC=gcc
CCC=g++
CXX=g++
FC=
AS=as

# Macros
CND_PLATFORM=GNU-Linux-x86
CND_CONF=Release
CND_DISTDIR=dist

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=build/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/src/jruby-cext.o \
	${OBJECTDIR}/src/funcall.o \
	${OBJECTDIR}/src/JLocalEnv.o \
	${OBJECTDIR}/src/array.o \
	${OBJECTDIR}/src/JString.o \
	${OBJECTDIR}/src/jruby.o \
	${OBJECTDIR}/src/JUtil.o \
	${OBJECTDIR}/src/data-object.o \
	${OBJECTDIR}/src/module.o \
	${OBJECTDIR}/src/ruby.o \
	${OBJECTDIR}/src/malloc.o \
	${OBJECTDIR}/src/JavaException.o \
	${OBJECTDIR}/src/hash.o \
	${OBJECTDIR}/src/Type.o \
	${OBJECTDIR}/src/invoke.o \
	${OBJECTDIR}/src/raise.o \
	${OBJECTDIR}/src/numeric.o \
	${OBJECTDIR}/src/Handle.o \
	${OBJECTDIR}/src/string.o \
	${OBJECTDIR}/src/class.o

# C Compiler Flags
CFLAGS=

# CC Compiler Flags
CCFLAGS=
CXXFLAGS=

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	${MAKE}  -f nbproject/Makefile-Release.mk dist/Release/GNU-Linux-x86/libjruby-cext-native.so

dist/Release/GNU-Linux-x86/libjruby-cext-native.so: ${OBJECTFILES}
	${MKDIR} -p dist/Release/GNU-Linux-x86
	${LINK.cc} -shared -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libjruby-cext-native.so -fPIC ${OBJECTFILES} ${LDLIBSOPTIONS} 

${OBJECTDIR}/src/jruby-cext.o: nbproject/Makefile-${CND_CONF}.mk src/jruby-cext.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/jruby-cext.o src/jruby-cext.cpp

${OBJECTDIR}/src/funcall.o: nbproject/Makefile-${CND_CONF}.mk src/funcall.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/funcall.o src/funcall.cpp

${OBJECTDIR}/src/JLocalEnv.o: nbproject/Makefile-${CND_CONF}.mk src/JLocalEnv.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/JLocalEnv.o src/JLocalEnv.cpp

${OBJECTDIR}/src/array.o: nbproject/Makefile-${CND_CONF}.mk src/array.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/array.o src/array.cpp

${OBJECTDIR}/src/JString.o: nbproject/Makefile-${CND_CONF}.mk src/JString.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/JString.o src/JString.cpp

${OBJECTDIR}/src/jruby.o: nbproject/Makefile-${CND_CONF}.mk src/jruby.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/jruby.o src/jruby.cpp

${OBJECTDIR}/src/JUtil.o: nbproject/Makefile-${CND_CONF}.mk src/JUtil.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/JUtil.o src/JUtil.cpp

${OBJECTDIR}/src/data-object.o: nbproject/Makefile-${CND_CONF}.mk src/data-object.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/data-object.o src/data-object.cpp

${OBJECTDIR}/src/module.o: nbproject/Makefile-${CND_CONF}.mk src/module.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/module.o src/module.cpp

${OBJECTDIR}/src/ruby.o: nbproject/Makefile-${CND_CONF}.mk src/ruby.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/ruby.o src/ruby.cpp

${OBJECTDIR}/src/malloc.o: nbproject/Makefile-${CND_CONF}.mk src/malloc.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/malloc.o src/malloc.cpp

${OBJECTDIR}/src/JavaException.o: nbproject/Makefile-${CND_CONF}.mk src/JavaException.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/JavaException.o src/JavaException.cpp

${OBJECTDIR}/src/hash.o: nbproject/Makefile-${CND_CONF}.mk src/hash.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/hash.o src/hash.cpp

${OBJECTDIR}/src/Type.o: nbproject/Makefile-${CND_CONF}.mk src/Type.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/Type.o src/Type.cpp

${OBJECTDIR}/src/invoke.o: nbproject/Makefile-${CND_CONF}.mk src/invoke.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/invoke.o src/invoke.cpp

${OBJECTDIR}/src/raise.o: nbproject/Makefile-${CND_CONF}.mk src/raise.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/raise.o src/raise.cpp

${OBJECTDIR}/src/numeric.o: nbproject/Makefile-${CND_CONF}.mk src/numeric.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/numeric.o src/numeric.cpp

${OBJECTDIR}/src/Handle.o: nbproject/Makefile-${CND_CONF}.mk src/Handle.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/Handle.o src/Handle.cpp

${OBJECTDIR}/src/string.o: nbproject/Makefile-${CND_CONF}.mk src/string.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/string.o src/string.cpp

${OBJECTDIR}/src/class.o: nbproject/Makefile-${CND_CONF}.mk src/class.cpp 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/class.o src/class.cpp

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r build/Release
	${RM} dist/Release/GNU-Linux-x86/libjruby-cext-native.so

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
