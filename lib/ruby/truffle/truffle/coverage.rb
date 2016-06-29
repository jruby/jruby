# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Coverage

  def self.start
    Truffle::Coverage.enable
  end

  def self.result
    result = peek_result
    Truffle::Coverage.disable
    result
  end

  def self.peek_result
    Truffle::Coverage.result_array.to_h
  end

end
