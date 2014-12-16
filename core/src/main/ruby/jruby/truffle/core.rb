# Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative 'core/main'
require_relative 'core/kernel'
require_relative 'core/float'
require_relative 'core/math'

require_relative 'core/rubinius/api/bootstrap/channel'
require_relative 'core/rubinius/api/common/bytearray'
require_relative 'core/rubinius/api/common/channel'
require_relative 'core/rubinius/api/common/thread'
require_relative 'core/rubinius/api/common/tuple'
require_relative 'core/rubinius/api/common/type'
require_relative 'core/rubinius/kernel/common/struct'

require_relative 'core/shims'
