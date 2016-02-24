# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

tagged_example "defined?(true) == 'true'", true
tagged_example "defined?(false) == 'false'", true
tagged_example "defined?(self) == 'self'", true
tagged_example "defined?(14) == 'expression'", true
tagged_example "defined?(14 + 2) == 'expression'", true
