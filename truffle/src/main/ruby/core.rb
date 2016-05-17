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
Truffle::Boot.require_core 'core/bootstrap/bignum'
Truffle::Boot.require_core 'core/bootstrap/channel'
Truffle::Boot.require_core 'core/character'
Truffle::Boot.require_core 'core/configuration'
Truffle::Boot.require_core 'core/bootstrap/dir'
Truffle::Boot.require_core 'core/bootstrap/false'
Truffle::Boot.require_core 'core/bootstrap/gc'
Truffle::Boot.require_core 'core/bootstrap/io'
Truffle::Boot.require_core 'core/bootstrap/kernel'
Truffle::Boot.require_core 'core/bootstrap/nil'
Truffle::Boot.require_core 'core/bootstrap/process'
Truffle::Boot.require_core 'core/bootstrap/regexp'
Truffle::Boot.require_core 'core/bootstrap/rubinius'
Truffle::Boot.require_core 'core/bootstrap/stat'
Truffle::Boot.require_core 'core/bootstrap/string'
Truffle::Boot.require_core 'core/bootstrap/symbol'
Truffle::Boot.require_core 'core/bootstrap/thread'
Truffle::Boot.require_core 'core/bootstrap/time'
Truffle::Boot.require_core 'core/bootstrap/true'
Truffle::Boot.require_core 'core/bootstrap/type'
Truffle::Boot.require_core 'core/weakref'

# Load platform

Truffle::Boot.require_core 'core/library'

Truffle::Boot.require_core 'core/platform/ffi'
Truffle::Boot.require_core 'core/platform/pointer_accessors'
Truffle::Boot.require_core 'core/platform/pointer'
Truffle::Boot.require_core 'core/platform/env'
Truffle::Boot.require_core 'core/platform/file'
Truffle::Boot.require_core 'core/platform/struct'

# Load common

Truffle::Boot.require_core 'core/common/string_mirror'
Truffle::Boot.require_core 'core/common/module'
Truffle::Boot.require_core 'core/common/proc'
Truffle::Boot.require_core 'core/common/enumerable_helper'
Truffle::Boot.require_core 'core/common/enumerable'
Truffle::Boot.require_core 'core/common/enumerator'
Truffle::Boot.require_core 'core/common/argf'
Truffle::Boot.require_core 'core/common/exception'
Truffle::Boot.require_core 'core/common/undefined'
Truffle::Boot.require_core 'core/common/type'
Truffle::Boot.require_core 'core/common/hash'
Truffle::Boot.require_core 'core/hash' # Our changes
Truffle::Boot.require_core 'core/common/array'
Truffle::Boot.require_core 'core/common/kernel'
Truffle::Boot.require_core 'core/common/identity_map'
Truffle::Boot.require_core 'core/common/comparable'
Truffle::Boot.require_core 'core/common/numeric_mirror'
Truffle::Boot.require_core 'core/common/numeric'
Truffle::Boot.require_core 'core/common/ctype'
Truffle::Boot.require_core 'core/common/integer'
Truffle::Boot.require_core 'core/common/bignum'
Truffle::Boot.require_core 'core/common/channel'
Truffle::Boot.require_core 'core/common/fixnum'
Truffle::Boot.require_core 'core/common/lru_cache'
Truffle::Boot.require_core 'core/common/encoding'
Truffle::Boot.require_core 'core/common/env'
Truffle::Boot.require_core 'core/common/errno'
Truffle::Boot.require_core 'core/common/false'
Truffle::Boot.require_core 'core/common/io'
Truffle::Boot.require_core 'core/common/file'
Truffle::Boot.require_core 'core/common/dir'
Truffle::Boot.require_core 'core/common/dir_glob'
Truffle::Boot.require_core 'core/common/file_test'
Truffle::Boot.require_core 'core/common/stat'
Truffle::Boot.require_core 'core/common/float'
Truffle::Boot.require_core 'core/common/immediate'
Truffle::Boot.require_core 'core/common/main'
Truffle::Boot.require_core 'core/common/marshal'
Truffle::Boot.require_core 'core/common/nil'
Truffle::Boot.require_core 'core/common/object_space'
Truffle::Boot.require_core 'core/common/string'
Truffle::Boot.require_core 'core/common/range_mirror'
Truffle::Boot.require_core 'core/common/range'
Truffle::Boot.require_core 'core/common/struct'
Truffle::Boot.require_core 'core/common/process'
Truffle::Boot.require_core 'core/common/process_mirror'
Truffle::Boot.require_core 'core/common/random'
Truffle::Boot.require_core 'core/common/regexp'
Truffle::Boot.require_core 'core/common/signal'
Truffle::Boot.require_core 'core/common/splitter'
Truffle::Boot.require_core 'core/common/symbol'
Truffle::Boot.require_core 'core/common/mutex'
Truffle::Boot.require_core 'core/common/thread'
Truffle::Boot.require_core 'core/common/throw_catch'
Truffle::Boot.require_core 'core/common/time'
Truffle::Boot.require_core 'core/common/true'
Truffle::Boot.require_core 'core/common/rational'
Truffle::Boot.require_core 'core/common/rationalizer'
Truffle::Boot.require_core 'core/common/complex'
Truffle::Boot.require_core 'core/common/complexifier'
Truffle::Boot.require_core 'core/common/gc'

# Load delta

Truffle::Boot.require_core 'core/delta/file'
Truffle::Boot.require_core 'core/delta/module'
Truffle::Boot.require_core 'core/delta/class'
Truffle::Boot.require_core 'core/delta/file_test'
Truffle::Boot.require_core 'core/delta/kernel'
Truffle::Boot.require_core 'core/delta/struct'
Truffle::Boot.require_core 'core/delta/ffi'

# Load JRuby+Truffle classes

Truffle::Boot.require_core 'core/array'
Truffle::Boot.require_core 'core/binding'
Truffle::Boot.require_core 'core/fixnum'
Truffle::Boot.require_core 'core/float'
Truffle::Boot.require_core 'core/kernel'
Truffle::Boot.require_core 'core/math'
Truffle::Boot.require_core 'core/method'
Truffle::Boot.require_core 'core/module'
Truffle::Boot.require_core 'core/signal'
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
