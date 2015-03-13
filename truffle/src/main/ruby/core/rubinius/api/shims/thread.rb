# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

class Thread

  # Rubinius seems to set @recursive_objects to {} in C++ - here we shim the
  # attribute reader to initialize it. __detect_outermost_recursion__ seems
  # to make things work - not sure why at this stage.

  def recursive_objects
    @recursive_objects ||= {:__detect_outermost_recursion__ => nil}
  end

end
