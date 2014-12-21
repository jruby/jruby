# Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative 'core/main'
require_relative 'core/config'
require_relative 'core/kernel'
require_relative 'core/float'
require_relative 'core/math'
require_relative 'core/thread'
require_relative 'core/module'

require_relative 'core/rubinius/api/compat/type'

require_relative 'core/rubinius/api/kernel/bootstrap/channel'
require_relative 'core/rubinius/api/kernel/common/bytearray'
require_relative 'core/rubinius/api/kernel/common/channel'
require_relative 'core/rubinius/api/kernel/common/thread'
require_relative 'core/rubinius/api/kernel/common/tuple'
require_relative 'core/rubinius/api/kernel/common/type'

require_relative 'core/rubinius/api/shims/lookuptable'
require_relative 'core/rubinius/api/shims/thread'
require_relative 'core/rubinius/api/shims/type'
require_relative 'core/rubinius/api/shims/enumerator'
require_relative 'core/rubinius/api/shims/undefined'

require_relative 'core/rubinius/kernel/common/undefined'
require_relative 'core/rubinius/kernel/common/kernel'
require_relative 'core/rubinius/kernel/common/array'
require_relative 'core/rubinius/kernel/common/struct'

require_relative 'core/shims'
