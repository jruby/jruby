# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

class String

  def %(args)
    if args.is_a? Hash
      sprintf(self, args)
    else
      sprintf(self, *args)
    end
  end

  def capitalize
    s = dup
    s.capitalize!
    s
  end

  def downcase
    s = dup
    s.downcase!
    s
  end

end
