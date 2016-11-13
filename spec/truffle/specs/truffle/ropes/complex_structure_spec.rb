# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1
# OTHER DEALINGS IN THE SOFTWARE.

require_relative '../../../../ruby/spec_helper'

describe "Truffle Rope complex structure" do

  [
      [(('abcd'*3)[1..-1]+('ABCD')), 'bcdabcdabcdABCD'],
      [(('abcd'*3)[1..-2]+('ABCD')), 'bcdabcdabcABCD'],
      [(('abcd'*3)[1..-3]+('ABCD')), 'bcdabcdabABCD'],
      [(('abcd'*3)[1..-4]+('ABCD')), 'bcdabcdaABCD'],
      [(('abcd'*3)[1..-5]+('ABCD')), 'bcdabcdABCD'],
  ].each_with_index do |(a, b), i|
    it format('%d: %s', i, b) do
      a.should == b
    end
  end

end
