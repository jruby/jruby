# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Truffle
  # Utility method for commenting out part of Rubinius's implementation and detailing why.  It helps clearly
  # demarcate things we want to omit from things Rubinius has left commented out and as such, should help compare
  # diffs when upgrading Rubinius core files.
  def self.omit(reason)
  end
end

require_relative 'core/pre'

# Load Rubinius API

require_relative 'core/rubinius/api/compat/type'
require_relative 'core/rubinius/api/kernel/common/thread'
require_relative 'core/rubinius/api/kernel/common/type'

# Patch rubinius-core-api to make it work for us

require_relative 'core/rubinius/api/shims/lookuptable'
require_relative 'core/rubinius/api/shims/rubinius'
require_relative 'core/rubinius/api/shims/thread'
require_relative 'core/rubinius/api/shims/tuple'
require_relative 'core/rubinius/api/shims/metrics'
require_relative 'core/rubinius/api/shims/hash'

# Rubinius primitives written in Ruby

require_relative 'core/rubinius/primitives'

# Load alpha.rb

require_relative 'core/rubinius/alpha'

# Load bootstrap (ordered according to Rubinius' load_order.txt)

require_relative 'core/rubinius/bootstrap/atomic'
require_relative 'core/rubinius/bootstrap/basic_object'
#require_relative 'core/rubinius/bootstrap/logger'
#require_relative 'core/rubinius/bootstrap/alias'
require_relative 'core/rubinius/bootstrap/mirror'
#require_relative 'core/rubinius/bootstrap/array_mirror'
#require_relative 'core/rubinius/bootstrap/array'
#require_relative 'core/rubinius/bootstrap/atomic'
require_relative 'core/rubinius/bootstrap/bignum'
#require_relative 'core/rubinius/bootstrap/block_environment'
#require_relative 'core/rubinius/bootstrap/byte_array'
#require_relative 'core/rubinius/bootstrap/call_site'
#require_relative 'core/rubinius/bootstrap/call_custom_cache'
#require_relative 'core/rubinius/bootstrap/channel'
require_relative 'core/rubinius/api/shims/channel'
require_relative 'core/rubinius/bootstrap/character'
#require_relative 'core/rubinius/bootstrap/class'
#require_relative 'core/rubinius/bootstrap/compact_lookup_table'
#require_relative 'core/rubinius/bootstrap/compiled_code'
require_relative 'core/rubinius/bootstrap/configuration'
#require_relative 'core/rubinius/bootstrap/constant_cache'
#require_relative 'core/rubinius/bootstrap/constant_scope'
#require_relative 'core/rubinius/bootstrap/constant_table'
require_relative 'core/rubinius/bootstrap/dir'
#require_relative 'core/rubinius/bootstrap/encoding'
#require_relative 'core/rubinius/bootstrap/exception'
#require_relative 'core/rubinius/bootstrap/executable'
require_relative 'core/rubinius/bootstrap/false'
#require_relative 'core/rubinius/bootstrap/fixnum'
require_relative 'core/rubinius/bootstrap/gc'
require_relative 'core/rubinius/bootstrap/io'
#require_relative 'core/rubinius/bootstrap/iseq'
#require_relative 'core/rubinius/bootstrap/jit'
require_relative 'core/rubinius/bootstrap/kernel'
#require_relative 'core/rubinius/bootstrap/lookup_table'
#require_relative 'core/rubinius/bootstrap/method_table'
#require_relative 'core/rubinius/bootstrap/mono_inline_cache'
require_relative 'core/rubinius/bootstrap/nil'
#require_relative 'core/rubinius/bootstrap/proc'
require_relative 'core/rubinius/bootstrap/process'
#require_relative 'core/rubinius/bootstrap/poly_inline_cache'
require_relative 'core/rubinius/bootstrap/regexp'
#require_relative 'core/rubinius/bootstrap/respond_to_cache'
require_relative 'core/rubinius/bootstrap/rubinius'
require_relative 'core/rubinius/bootstrap/stat'
require_relative 'core/rubinius/bootstrap/string'
require_relative 'core/rubinius/bootstrap/symbol'
require_relative 'core/rubinius/bootstrap/thread'
require_relative 'core/rubinius/api/shims/thread_bootstrap'
#require_relative 'core/rubinius/bootstrap/thunk'
require_relative 'core/rubinius/bootstrap/time'
require_relative 'core/rubinius/bootstrap/true'
#require_relative 'core/rubinius/bootstrap/tuple'
require_relative 'core/rubinius/bootstrap/type'
#require_relative 'core/rubinius/bootstrap/variable_scope'
#require_relative 'core/rubinius/bootstrap/vm'
require_relative 'core/rubinius/bootstrap/weakref'

# Load platform (ordered according to Rubinius' load_order.txt)

require_relative 'core/library'

require_relative 'core/rubinius/platform/ffi'
#require_relative 'core/rubinius/platform/enum'
#require_relative 'core/rubinius/platform/library'
require_relative 'core/rubinius/platform/pointer_accessors'
require_relative 'core/rubinius/platform/pointer'
require_relative 'core/rubinius/platform/env'
require_relative 'core/rubinius/platform/file'
#require_relative 'core/rubinius/platform/math'
#require_relative 'core/rubinius/platform/posix'
require_relative 'core/rubinius/platform/struct'
#require_relative 'core/rubinius/platform/union'

# Load common (ordered according to Rubinius' load_order.txt)

#require_relative 'core/rubinius/common/basic_object'
require_relative 'core/rubinius/common/string_mirror'
require_relative 'core/rubinius/api/shims/string_mirror'
#require_relative 'core/rubinius/common/class'
#require_relative 'core/rubinius/common/autoload'
require_relative 'core/rubinius/common/module'
require_relative 'core/rubinius/api/shims/module'
#require_relative 'core/rubinius/common/binding'
require_relative 'core/rubinius/common/proc'
require_relative 'core/rubinius/common/enumerable_helper'
require_relative 'core/rubinius/common/enumerable'
require_relative 'core/rubinius/common/enumerator'
require_relative 'core/rubinius/common/argf'
require_relative 'core/rubinius/api/shims/argf'
#require_relative 'core/rubinius/common/tuple'
require_relative 'core/rubinius/common/exception'
require_relative 'core/rubinius/api/shims/exception'
require_relative 'core/rubinius/common/undefined'
require_relative 'core/rubinius/common/type'
require_relative 'core/rubinius/common/hash'
require_relative 'core/hash' # Our changes
#require_relative 'core/rubinius/common/hash_hamt'
require_relative 'core/rubinius/common/array'
require_relative 'core/rubinius/api/shims/array'
require_relative 'core/rubinius/common/kernel'
require_relative 'core/rubinius/api/shims/kernel'
require_relative 'core/rubinius/common/identity_map'
#require_relative 'core/rubinius/common/loaded_features'
#require_relative 'core/rubinius/common/global'
#require_relative 'core/rubinius/common/backtrace'
require_relative 'core/rubinius/common/comparable'
require_relative 'core/rubinius/common/numeric_mirror'
require_relative 'core/rubinius/common/numeric'
require_relative 'core/rubinius/common/ctype'
require_relative 'core/rubinius/common/integer'
require_relative 'core/rubinius/common/bignum'
#require_relative 'core/rubinius/common/block_environment'
#require_relative 'core/rubinius/common/byte_array'
require_relative 'core/rubinius/common/channel'
#require_relative 'core/rubinius/common/executable'
#require_relative 'core/rubinius/common/constant_scope'
#require_relative 'core/rubinius/common/hook'
#require_relative 'core/rubinius/common/code_loader'
#require_relative 'core/rubinius/common/compiled_code'
#require_relative 'core/rubinius/common/continuation'
#require_relative 'core/rubinius/common/delegated_method'
require_relative 'core/rubinius/common/fixnum'
require_relative 'core/rubinius/api/shims/fixnum'
require_relative 'core/rubinius/common/lru_cache'
require_relative 'core/rubinius/api/shims/encoding'
require_relative 'core/rubinius/common/encoding'
require_relative 'core/rubinius/common/env'
ENV = Rubinius::EnvironmentVariables.new
require_relative 'core/rubinius/common/errno'
#require_relative 'core/rubinius/common/eval'
require_relative 'core/rubinius/common/false'
#require_relative 'core/rubinius/common/fiber'
require_relative 'core/rubinius/common/io'
require_relative 'core/rubinius/api/shims/io'
require_relative 'core/rubinius/common/file'
require_relative 'core/rubinius/common/dir'
require_relative 'core/rubinius/common/dir_glob'
require_relative 'core/rubinius/common/file_test'
require_relative 'core/rubinius/common/stat'
require_relative 'core/rubinius/api/shims/stat'
require_relative 'core/rubinius/common/float'
require_relative 'core/rubinius/common/immediate'
#require_relative 'core/rubinius/common/location'
#require_relative 'core/rubinius/common/lookup_table'
require_relative 'core/rubinius/common/main'
require_relative 'core/rubinius/common/marshal'
require_relative 'core/rubinius/api/shims/marshal'
#require_relative 'core/rubinius/common/math'
#require_relative 'core/rubinius/common/method'
#require_relative 'core/rubinius/common/method_equality'
#require_relative 'core/rubinius/common/method_table'
#require_relative 'core/rubinius/common/missing_method'
#require_relative 'core/rubinius/common/native_method'
require_relative 'core/rubinius/common/nil'
require_relative 'core/rubinius/common/object_space'
require_relative 'core/rubinius/common/string'
require_relative 'core/rubinius/common/range_mirror'
require_relative 'core/rubinius/api/shims/range_mirror'
require_relative 'core/rubinius/common/range'
require_relative 'core/rubinius/api/shims/range'
require_relative 'core/rubinius/common/struct'
require_relative 'core/rubinius/common/process'
require_relative 'core/rubinius/common/process_mirror'
require_relative 'core/rubinius/common/random'
require_relative 'core/rubinius/common/regexp'
require_relative 'core/rubinius/common/signal'
require_relative 'core/rubinius/common/splitter'
#require_relative 'core/rubinius/common/sprinter'
require_relative 'core/rubinius/common/symbol'
require_relative 'core/rubinius/common/mutex'
require_relative 'core/rubinius/common/thread'
#require_relative 'core/rubinius/common/thread_group'
require_relative 'core/rubinius/common/throw_catch'
require_relative 'core/rubinius/common/time'
require_relative 'core/rubinius/api/shims/time'
require_relative 'core/rubinius/common/true'
#require_relative 'core/rubinius/common/variable_scope'
#require_relative 'core/rubinius/common/capi'
require_relative 'core/rubinius/common/rational'
require_relative 'core/rubinius/common/rationalizer'
require_relative 'core/rubinius/common/complex'
require_relative 'core/rubinius/common/complexifier'
require_relative 'core/rubinius/common/gc'

# Load delta (ordered according to Rubinius' load_order.txt)

#require_relative 'core/rubinius/delta/ctype'
#require_relative 'core/rubinius/delta/exception'
require_relative 'core/rubinius/delta/file'
#require_relative 'core/rubinius/delta/rubinius'
#require_relative 'core/rubinius/delta/runtime'
require_relative 'core/rubinius/delta/module'
require_relative 'core/rubinius/delta/class'
require_relative 'core/rubinius/delta/file_test'
require_relative 'core/rubinius/delta/kernel'
#require_relative 'core/rubinius/delta/math'
#require_relative 'core/rubinius/delta/options'
#require_relative 'core/rubinius/delta/stats'
#require_relative 'core/rubinius/delta/signal'
require_relative 'core/rubinius/delta/struct'
#require_relative 'core/rubinius/delta/thread'
#require_relative 'core/rubinius/delta/code_loader'
#require_relative 'core/rubinius/delta/fsevent'
#require_relative 'core/rubinius/delta/console'
require_relative 'core/rubinius/delta/ffi'
#require_relative 'core/rubinius/delta/ruby_constants'
#require_relative 'core/rubinius/delta/pack'
#require_relative 'core/rubinius/delta/metrics'

# Load JRuby+Truffle classes

require_relative 'core/array'
require_relative 'core/binding'
require_relative 'core/fixnum'
require_relative 'core/float'
require_relative 'core/kernel'
require_relative 'core/math'
require_relative 'core/method'
require_relative 'core/module'
require_relative 'core/signal'
require_relative 'core/string'
require_relative 'core/thread'
require_relative 'core/unbound_method'
require_relative 'core/type'

# Dirty fixes we'd like to get rid of soon
require_relative 'core/shims'

# Load JRuby+Truffle specific classes

require_relative 'core/truffle/attachments'
require_relative 'core/truffle/debug'
require_relative 'core/truffle/runtime'
require_relative 'core/truffle/truffle'
require_relative 'core/truffle/interop'

# Start running Ruby code outside classes

require_relative 'core/config'
require_relative 'core/main'

# JRuby+Truffle C extension support
require_relative 'core/truffle/cext/cext'
require_relative 'core/truffle/cext/mkmf'
require_relative 'core/truffle/cext/require'

require_relative 'core/post'

