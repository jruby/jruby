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
require_relative 'core/rubinius/api/shims/array'
require_relative 'core/rubinius/api/shims/rubinius'
require_relative 'core/rubinius/api/shims/lookuptable'
require_relative 'core/rubinius/api/shims/thread'
require_relative 'core/rubinius/api/shims/tuple'
require_relative 'core/rubinius/api/shims/undefined'
require_relative 'core/rubinius/api/shims/metrics'

# Load bootstrap (ordered according to Rubinius' load_order.txt)
require_relative 'core/rubinius/kernel/bootstrap/basic_object'
require_relative 'core/rubinius/kernel/bootstrap/false'
require_relative 'core/rubinius/kernel/bootstrap/gc'
require_relative 'core/rubinius/kernel/bootstrap/kernel'
require_relative 'core/rubinius/kernel/bootstrap/nil'
require_relative 'core/rubinius/kernel/bootstrap/regexp'
require_relative 'core/rubinius/kernel/bootstrap/rubinius'
require_relative 'core/rubinius/kernel/bootstrap/string'
require_relative 'core/rubinius/kernel/bootstrap/symbol'
require_relative 'core/rubinius/kernel/bootstrap/time'
require_relative 'core/rubinius/kernel/bootstrap/true'
require_relative 'core/rubinius/kernel/bootstrap/type'

# Load common (ordered according to Rubinius' load_order.txt)
require_relative 'core/rubinius/kernel/common/enumerator'
require_relative 'core/rubinius/kernel/common/enumerable'
require_relative 'core/rubinius/kernel/common/exception'
require_relative 'core/rubinius/kernel/common/undefined'
require_relative 'core/rubinius/kernel/common/type'
require_relative 'core/rubinius/kernel/common/array'
require_relative 'core/rubinius/kernel/common/kernel'
require_relative 'core/rubinius/kernel/common/comparable'
require_relative 'core/rubinius/kernel/common/numeric'
require_relative 'core/rubinius/kernel/common/identity_map'
require_relative 'core/rubinius/kernel/common/integer'
require_relative 'core/rubinius/kernel/common/fixnum'
require_relative 'core/rubinius/kernel/common/false'
require_relative 'core/rubinius/kernel/common/float'
require_relative 'core/rubinius/kernel/common/immediate'
require_relative 'core/rubinius/kernel/common/main'
require_relative 'core/rubinius/kernel/common/marshal'
require_relative 'core/rubinius/kernel/common/nil'
require_relative 'core/rubinius/kernel/common/object_space'
require_relative 'core/rubinius/kernel/common/proc'
require_relative 'core/rubinius/kernel/common/string'
require_relative 'core/rubinius/kernel/common/struct'
require_relative 'core/rubinius/kernel/common/regexp'
require_relative 'core/rubinius/kernel/common/signal'
require_relative 'core/rubinius/kernel/common/mutex'
require_relative 'core/rubinius/kernel/common/time'
require_relative 'core/rubinius/kernel/common/true'

require_relative 'core/rubinius/kernel/common/rational'
require_relative 'core/rubinius/kernel/common/complex'
require_relative 'core/rubinius/kernel/common/gc'

# Load JRuby+Truffle classes
require_relative 'core/array'
require_relative 'core/fixnum'
require_relative 'core/float'
require_relative 'core/hash'
require_relative 'core/kernel'
require_relative 'core/math'
require_relative 'core/method'
require_relative 'core/module'
require_relative 'core/string'
require_relative 'core/thread'
require_relative 'core/unbound_method'

require_relative 'core/shims'

# Start running Ruby code outside classes
require_relative 'core/config'
require_relative 'core/main'
