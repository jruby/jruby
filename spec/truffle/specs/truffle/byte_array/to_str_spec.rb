# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1
# OTHER DEALINGS IN THE SOFTWARE.

require_relative '../../../../ruby/spec_helper'

describe 'Rubinius::ByteArray.to_str' do

  before :each do
    @byte_array = Rubinius::ByteArray.new(3, 0)
    @byte_array[0] = 'a'.ord
    @byte_array[1] = 'b'.ord
    @byte_array[2] = 'c'.ord
  end

  describe 'with ASCII characters' do

    it 'should be an ASCII-8BIT (binary) string' do
      @byte_array.to_str.encoding.should == Encoding::ASCII_8BIT
      @byte_array.to_str.should == 'abc'
    end

  end

end