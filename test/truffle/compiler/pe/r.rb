# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Either R's eval or R expressions in general do not PE yet
tagged main_thread example "Truffle::Interop.eval('application/x-r', '14 + 2')", 16.0
