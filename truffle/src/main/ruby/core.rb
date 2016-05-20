# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

Truffle::Boot.require_core 'core/pre'

# Rubinius primitives written in Ruby

Truffle::Boot.require_core 'core/primitives'

# Load alpha.rb

Truffle::Boot.require_core 'core/alpha'

# Load bootstrap

Truffle::Boot.require_core 'core/tuple'
Truffle::Boot.require_core 'core/lookuptable'
Truffle::Boot.require_core 'core/atomic'
Truffle::Boot.require_core 'core/basic_object'
Truffle::Boot.require_core 'core/mirror'
Truffle::Boot.require_core 'core/bignum'
Truffle::Boot.require_core 'core/channel'
Truffle::Boot.require_core 'core/character'
Truffle::Boot.require_core 'core/configuration'
Truffle::Boot.require_core 'core/bootstrap/dir'
Truffle::Boot.require_core 'core/false'
Truffle::Boot.require_core 'core/gc'
Truffle::Boot.require_core 'core/bootstrap/io'
Truffle::Boot.require_core 'core/bootstrap/kernel'
Truffle::Boot.require_core 'core/nil'
Truffle::Boot.require_core 'core/bootstrap/process'
Truffle::Boot.require_core 'core/bootstrap/regexp'
Truffle::Boot.require_core 'core/bootstrap/rubinius'
Truffle::Boot.require_core 'core/bootstrap/stat'
Truffle::Boot.require_core 'core/bootstrap/string'
Truffle::Boot.require_core 'core/bootstrap/thread'
Truffle::Boot.require_core 'core/true'
Truffle::Boot.require_core 'core/bootstrap/type'
Truffle::Boot.require_core 'core/weakref'

# Load platform

Truffle::Boot.require_core 'core/library'

Truffle::Boot.require_core 'core/ffi'
Truffle::Boot.require_core 'core/pointer_accessors'
Truffle::Boot.require_core 'core/pointer'
Truffle::Boot.require_core 'core/platform/file'
Truffle::Boot.require_core 'core/platform/struct'

# Load common

Truffle::Boot.require_core 'core/immediate'
Truffle::Boot.require_core 'core/string_mirror'
Truffle::Boot.require_core 'core/common/module'
Truffle::Boot.require_core 'core/proc'
Truffle::Boot.require_core 'core/enumerable_helper'
Truffle::Boot.require_core 'core/enumerable'
Truffle::Boot.require_core 'core/enumerator'
Truffle::Boot.require_core 'core/argf'
Truffle::Boot.require_core 'core/exception'
Truffle::Boot.require_core 'core/undefined'
Truffle::Boot.require_core 'core/common/type'
Truffle::Boot.require_core 'core/hash'
Truffle::Boot.require_core 'core/array'
Truffle::Boot.require_core 'core/common/kernel'
Truffle::Boot.require_core 'core/identity_map'
Truffle::Boot.require_core 'core/comparable'
Truffle::Boot.require_core 'core/numeric_mirror'
Truffle::Boot.require_core 'core/numeric'
Truffle::Boot.require_core 'core/ctype'
Truffle::Boot.require_core 'core/integer'
Truffle::Boot.require_core 'core/fixnum'
Truffle::Boot.require_core 'core/lru_cache'
Truffle::Boot.require_core 'core/encoding'
Truffle::Boot.require_core 'core/env'
Truffle::Boot.require_core 'core/errno'
Truffle::Boot.require_core 'core/common/io'
Truffle::Boot.require_core 'core/common/file'
Truffle::Boot.require_core 'core/common/dir'
Truffle::Boot.require_core 'core/dir_glob'
Truffle::Boot.require_core 'core/file_test'
Truffle::Boot.require_core 'core/common/stat'
Truffle::Boot.require_core 'core/float'
Truffle::Boot.require_core 'core/marshal'
Truffle::Boot.require_core 'core/object_space'
Truffle::Boot.require_core 'core/common/string'
Truffle::Boot.require_core 'core/range_mirror'
Truffle::Boot.require_core 'core/range'
Truffle::Boot.require_core 'core/common/struct'
Truffle::Boot.require_core 'core/common/process'
Truffle::Boot.require_core 'core/process_mirror'
Truffle::Boot.require_core 'core/random'
Truffle::Boot.require_core 'core/common/regexp'
Truffle::Boot.require_core 'core/signal'
Truffle::Boot.require_core 'core/splitter'
Truffle::Boot.require_core 'core/symbol'
Truffle::Boot.require_core 'core/mutex'
Truffle::Boot.require_core 'core/common/thread'
Truffle::Boot.require_core 'core/throw_catch'
Truffle::Boot.require_core 'core/time'
Truffle::Boot.require_core 'core/rational'
Truffle::Boot.require_core 'core/rationalizer'
Truffle::Boot.require_core 'core/complex'
Truffle::Boot.require_core 'core/complexifier'

# Load delta

Truffle::Boot.require_core 'core/delta/file'
Truffle::Boot.require_core 'core/delta/module'
Truffle::Boot.require_core 'core/class'
Truffle::Boot.require_core 'core/delta/kernel'
Truffle::Boot.require_core 'core/delta/struct'

# Load JRuby+Truffle classes

Truffle::Boot.require_core 'core/binding'
Truffle::Boot.require_core 'core/kernel'
Truffle::Boot.require_core 'core/math'
Truffle::Boot.require_core 'core/method'
Truffle::Boot.require_core 'core/module'
Truffle::Boot.require_core 'core/string'
Truffle::Boot.require_core 'core/thread'
Truffle::Boot.require_core 'core/unbound_method'
Truffle::Boot.require_core 'core/type'

# Dirty fixes we'd like to get rid of soon
Truffle::Boot.require_core 'core/shims'

# Load JRuby+Truffle specific classes

Truffle::Boot.require_core 'core/truffle/attachments'
Truffle::Boot.require_core 'core/truffle/debug'
Truffle::Boot.require_core 'core/truffle/cext'
Truffle::Boot.require_core 'core/truffle/interop'

# Start running Ruby code outside classes

Truffle::Boot.require_core 'core/rbconfig'
Truffle::Boot.require_core 'core/main'

Truffle::Boot.require_core 'core/post'
