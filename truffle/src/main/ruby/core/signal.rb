# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Signal

  # Fill the Names and Numbers Hash.
  SIGNAL_LIST.each do |name, number|
    Names[name] = number
    Numbers[number] = name
  end
  remove_const :SIGNAL_LIST

end
