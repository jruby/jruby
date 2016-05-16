# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

Truffle::Boot.require_core 'core/pre'

# Patch rubinius-core-api to make it work for us

Truffle::Boot.require_core 'core/rubinius/api/shims/lookuptable'
Truffle::Boot.require_core 'core/rubinius/api/shims/metrics'
Truffle::Boot.require_core 'core/rubinius/api/shims/hash'

# Rubinius primitives written in Ruby

Truffle::Boot.require_core 'core/rubinius/primitives'

# Load alpha.rb

Truffle::Boot.require_core 'core/rubinius/alpha'

# Load bootstrap

Truffle::Boot.require_core 'core/rubinius/bootstrap/tuple'
Truffle::Boot.require_core 'core/rubinius/bootstrap/atomic'
Truffle::Boot.require_core 'core/rubinius/bootstrap/basic_object'
Truffle::Boot.require_core 'core/rubinius/bootstrap/mirror'
Truffle::Boot.require_core 'core/rubinius/bootstrap/bignum'
Truffle::Boot.require_core 'core/rubinius/bootstrap/channel'
Truffle::Boot.require_core 'core/rubinius/bootstrap/character'
Truffle::Boot.require_core 'core/rubinius/bootstrap/configuration'
Truffle::Boot.require_core 'core/rubinius/bootstrap/dir'
Truffle::Boot.require_core 'core/rubinius/bootstrap/false'
Truffle::Boot.require_core 'core/rubinius/bootstrap/gc'
Truffle::Boot.require_core 'core/rubinius/bootstrap/io'
Truffle::Boot.require_core 'core/rubinius/bootstrap/kernel'
Truffle::Boot.require_core 'core/rubinius/bootstrap/nil'
Truffle::Boot.require_core 'core/rubinius/bootstrap/process'
Truffle::Boot.require_core 'core/rubinius/bootstrap/regexp'
Truffle::Boot.require_core 'core/rubinius/bootstrap/rubinius'
Truffle::Boot.require_core 'core/rubinius/bootstrap/stat'
Truffle::Boot.require_core 'core/rubinius/bootstrap/string'
Truffle::Boot.require_core 'core/rubinius/bootstrap/symbol'
Truffle::Boot.require_core 'core/rubinius/bootstrap/thread'
Truffle::Boot.require_core 'core/rubinius/bootstrap/time'
Truffle::Boot.require_core 'core/rubinius/bootstrap/true'
Truffle::Boot.require_core 'core/rubinius/bootstrap/type'
Truffle::Boot.require_core 'core/rubinius/bootstrap/weakref'

# Load platform

Truffle::Boot.require_core 'core/library'

Truffle::Boot.require_core 'core/rubinius/platform/ffi'
Truffle::Boot.require_core 'core/rubinius/platform/pointer_accessors'
Truffle::Boot.require_core 'core/rubinius/platform/pointer'
Truffle::Boot.require_core 'core/rubinius/platform/env'
Truffle::Boot.require_core 'core/rubinius/platform/file'
Truffle::Boot.require_core 'core/rubinius/platform/struct'

# Load common

Truffle::Boot.require_core 'core/rubinius/common/string_mirror'
Truffle::Boot.require_core 'core/rubinius/common/module'
Truffle::Boot.require_core 'core/rubinius/common/proc'
Truffle::Boot.require_core 'core/rubinius/common/enumerable_helper'
Truffle::Boot.require_core 'core/rubinius/common/enumerable'
Truffle::Boot.require_core 'core/rubinius/common/enumerator'
Truffle::Boot.require_core 'core/rubinius/common/argf'
Truffle::Boot.require_core 'core/rubinius/common/exception'
Truffle::Boot.require_core 'core/rubinius/common/undefined'
Truffle::Boot.require_core 'core/rubinius/common/type'
Truffle::Boot.require_core 'core/rubinius/common/hash'
Truffle::Boot.require_core 'core/hash' # Our changes
Truffle::Boot.require_core 'core/rubinius/common/array'
Truffle::Boot.require_core 'core/rubinius/api/shims/array'
Truffle::Boot.require_core 'core/rubinius/common/kernel'
Truffle::Boot.require_core 'core/rubinius/common/identity_map'
Truffle::Boot.require_core 'core/rubinius/common/comparable'
Truffle::Boot.require_core 'core/rubinius/common/numeric_mirror'
Truffle::Boot.require_core 'core/rubinius/common/numeric'
Truffle::Boot.require_core 'core/rubinius/common/ctype'
Truffle::Boot.require_core 'core/rubinius/common/integer'
Truffle::Boot.require_core 'core/rubinius/common/bignum'
Truffle::Boot.require_core 'core/rubinius/common/channel'
Truffle::Boot.require_core 'core/rubinius/common/fixnum'
Truffle::Boot.require_core 'core/rubinius/common/lru_cache'
Truffle::Boot.require_core 'core/rubinius/common/encoding'
Truffle::Boot.require_core 'core/rubinius/common/env'
Truffle::Boot.require_core 'core/rubinius/common/errno'
Truffle::Boot.require_core 'core/rubinius/common/false'
Truffle::Boot.require_core 'core/rubinius/common/io'
Truffle::Boot.require_core 'core/rubinius/common/file'
Truffle::Boot.require_core 'core/rubinius/common/dir'
Truffle::Boot.require_core 'core/rubinius/common/dir_glob'
Truffle::Boot.require_core 'core/rubinius/common/file_test'
Truffle::Boot.require_core 'core/rubinius/common/stat'
Truffle::Boot.require_core 'core/rubinius/common/float'
Truffle::Boot.require_core 'core/rubinius/common/immediate'
Truffle::Boot.require_core 'core/rubinius/common/main'
Truffle::Boot.require_core 'core/rubinius/common/marshal'
Truffle::Boot.require_core 'core/rubinius/common/nil'
Truffle::Boot.require_core 'core/rubinius/common/object_space'
Truffle::Boot.require_core 'core/rubinius/common/string'
Truffle::Boot.require_core 'core/rubinius/common/range_mirror'
Truffle::Boot.require_core 'core/rubinius/common/range'
Truffle::Boot.require_core 'core/rubinius/common/struct'
Truffle::Boot.require_core 'core/rubinius/common/process'
Truffle::Boot.require_core 'core/rubinius/common/process_mirror'
Truffle::Boot.require_core 'core/rubinius/common/random'
Truffle::Boot.require_core 'core/rubinius/common/regexp'
Truffle::Boot.require_core 'core/rubinius/common/signal'
Truffle::Boot.require_core 'core/rubinius/common/splitter'
Truffle::Boot.require_core 'core/rubinius/common/symbol'
Truffle::Boot.require_core 'core/rubinius/common/mutex'
Truffle::Boot.require_core 'core/rubinius/common/thread'
Truffle::Boot.require_core 'core/rubinius/common/throw_catch'
Truffle::Boot.require_core 'core/rubinius/common/time'
Truffle::Boot.require_core 'core/rubinius/common/true'
Truffle::Boot.require_core 'core/rubinius/common/rational'
Truffle::Boot.require_core 'core/rubinius/common/rationalizer'
Truffle::Boot.require_core 'core/rubinius/common/complex'
Truffle::Boot.require_core 'core/rubinius/common/complexifier'
Truffle::Boot.require_core 'core/rubinius/common/gc'

# Load delta

Truffle::Boot.require_core 'core/rubinius/delta/file'
Truffle::Boot.require_core 'core/rubinius/delta/module'
Truffle::Boot.require_core 'core/rubinius/delta/class'
Truffle::Boot.require_core 'core/rubinius/delta/file_test'
Truffle::Boot.require_core 'core/rubinius/delta/kernel'
Truffle::Boot.require_core 'core/rubinius/delta/struct'
Truffle::Boot.require_core 'core/rubinius/delta/ffi'

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
