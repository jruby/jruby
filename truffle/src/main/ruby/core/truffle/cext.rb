# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Truffle
  module CExt
    module_function

    def supported?
      Interop.mime_type_supported?('application/x-sulong-library')
    end
    
    def Qfalse
      false
    end
    
    def rb_define_module(name)
      Object.const_set(name, Module.new)
    end
  
  end
end
