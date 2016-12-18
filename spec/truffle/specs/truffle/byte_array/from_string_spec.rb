# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1
# OTHER DEALINGS IN THE SOFTWARE.

require_relative '../../../../ruby/spec_helper'

describe 'Rubinius::ByteArray.from_string' do

  before :each do
    @string = "\u{6666}"
    @byte_array = Rubinius::ByteArray.from_string(@string)
  end

  describe 'with variable-width character string' do

    it 'should have the same byte length as the string' do
      @byte_array.size.should == @string.bytesize
    end

    it 'should have the same bytes as the string' do
      @string.bytes.each_with_index do |value, index|
        @byte_array[index].should == value
      end
    end

  end

end