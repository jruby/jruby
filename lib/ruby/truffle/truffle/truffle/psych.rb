# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Truffle::Psych
  
  def self.libyaml_version
    # TODO CS 23-Sep-15 hardcoded this for now - use resources to read
    [1, 14, 0]
  end

end

Psych = Truffle::Psych
