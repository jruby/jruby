# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

class Rubinius::Stat

  # Process.groups only return supplemental groups, so we need to check if gid/egid match too.
  def grpowned?
    gid = gid()
    return true if gid == Process.gid || gid == Process.egid
    Process.groups.include?(gid)
  end

end
