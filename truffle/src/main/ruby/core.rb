# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Load Rubinius API
require_relative 'core/rubinius/api/compat/type'
require_relative 'core/rubinius/api/kernel/common/thread'
require_relative 'core/rubinius/api/kernel/common/type'

# Patch rubinius-core-api to make it work for us
require_relative 'core/rubinius/api/shims/lookuptable'
require_relative 'core/rubinius/api/shims/array'
require_relative 'core/rubinius/api/shims/rubinius'
require_relative 'core/rubinius/api/shims/thread'
require_relative 'core/rubinius/api/shims/tuple'
require_relative 'core/rubinius/api/shims/undefined'
require_relative 'core/rubinius/api/shims/metrics'
require_relative 'core/rubinius/api/shims/config'
require_relative 'core/rubinius/api/shims/module'

# Load bootstrap (ordered according to Rubinius' load_order.txt)
require_relative 'core/rubinius/bootstrap/basic_object'
require_relative 'core/rubinius/bootstrap/mirror'
require_relative 'core/rubinius/bootstrap/character'
require_relative 'core/rubinius/bootstrap/false'
require_relative 'core/rubinius/bootstrap/gc'
require_relative 'core/rubinius/bootstrap/kernel'
require_relative 'core/rubinius/bootstrap/nil'
require_relative 'core/rubinius/bootstrap/process'
require_relative 'core/rubinius/bootstrap/regexp'
require_relative 'core/rubinius/bootstrap/rubinius'
require_relative 'core/rubinius/bootstrap/string'
require_relative 'core/rubinius/bootstrap/symbol'
require_relative 'core/rubinius/bootstrap/time'
require_relative 'core/rubinius/bootstrap/true'
require_relative 'core/rubinius/bootstrap/type'

# Load common (ordered according to Rubinius' load_order.txt)
require_relative 'core/rubinius/common/string_mirror'
require_relative 'core/rubinius/common/enumerator'
require_relative 'core/rubinius/common/enumerable'
require_relative 'core/rubinius/common/exception'
require_relative 'core/rubinius/common/undefined'
require_relative 'core/rubinius/common/type'
require_relative 'core/rubinius/common/hash'
require_relative 'core/rubinius/common/array'
require_relative 'core/rubinius/common/kernel'
require_relative 'core/rubinius/common/identity_map'
require_relative 'core/rubinius/common/comparable'
require_relative 'core/rubinius/common/numeric'
require_relative 'core/rubinius/common/ctype'
require_relative 'core/rubinius/common/integer'
require_relative 'core/rubinius/common/bignum'
require_relative 'core/rubinius/common/fixnum'
require_relative 'core/rubinius/api/shims/encoding'
require_relative 'core/rubinius/common/encoding'
require_relative 'core/rubinius/common/false'
require_relative 'core/rubinius/common/float'
require_relative 'core/rubinius/common/immediate'
require_relative 'core/rubinius/common/main'
require_relative 'core/rubinius/common/marshal'
require_relative 'core/rubinius/common/nil'
require_relative 'core/rubinius/common/object_space'
require_relative 'core/rubinius/common/proc'
require_relative 'core/rubinius/common/string'
require_relative 'core/rubinius/common/range'
require_relative 'core/rubinius/common/struct'
require_relative 'core/rubinius/common/process'
require_relative 'core/rubinius/common/symbol'
require_relative 'core/rubinius/common/regexp'
require_relative 'core/rubinius/common/signal'
require_relative 'core/rubinius/common/splitter'
require_relative 'core/rubinius/common/mutex'
require_relative 'core/rubinius/common/thread'
require_relative 'core/rubinius/common/throw_catch'
require_relative 'core/rubinius/common/time'
require_relative 'core/rubinius/common/true'
require_relative 'core/rubinius/common/random'
require_relative 'core/rubinius/common/rational'
require_relative 'core/rubinius/common/rationalizer'
require_relative 'core/rubinius/common/complex'
require_relative 'core/rubinius/common/complexifier'
require_relative 'core/rubinius/common/gc'

# Load delta (ordered according to Rubinius' load_order.txt)
require_relative 'core/rubinius/delta/class'
require_relative 'core/rubinius/delta/struct'

# Load JRuby+Truffle classes
require_relative 'core/array'
require_relative 'core/fixnum'
require_relative 'core/float'
require_relative 'core/hash'
require_relative 'core/kernel'
require_relative 'core/math'
require_relative 'core/method'
require_relative 'core/signal'
require_relative 'core/string'
require_relative 'core/thread'
require_relative 'core/unbound_method'

require_relative 'core/shims'

# Load JRuby+Truffle specific classes
require_relative 'core/truffle/truffle'
require_relative 'core/truffle/debug'

# Start running Ruby code outside classes
require_relative 'core/config'
require_relative 'core/main'
