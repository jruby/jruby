# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1
# OTHER DEALINGS IN THE SOFTWARE.

require_relative '../../../../ruby/spec_helper'

describe 'Rubinius::ByteArray#append' do

  before :each do
    @byte_array = Rubinius::ByteArray.new(16, 0xff)
  end

  describe 'with string' do

    before :each  do
      @string = "\u{6666}"
    end

    it "should add the string's bytes to the end of the byte array" do
      original_size = @byte_array.size
      @byte_array.append(@string)

      @byte_array.size.should == original_size + @string.bytesize

      @string.bytes.each_with_index do |value, index|
        @byte_array[original_size + index].should == value
      end
    end

  end

  describe 'with byte array' do

    before :each do
      @other_byte_array = Rubinius::ByteArray.new(3, 0xaa)
    end

    it "should add the other byte array's bytes to the end of the byte array" do
      original_size = @byte_array.size
      @byte_array.append(@other_byte_array)

      @byte_array.size.should == original_size + @other_byte_array.size

      @other_byte_array.size.times do |index|
        @byte_array[original_size + index].should == @other_byte_array[index]
      end
    end

  end

end