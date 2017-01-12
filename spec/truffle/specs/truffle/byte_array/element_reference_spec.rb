# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1
# OTHER DEALINGS IN THE SOFTWARE.

require_relative '../../../../ruby/spec_helper'

describe 'Rubinius::ByteArray#[]' do

  before :each do
    @byte_array = Rubinius::ByteArray.new(16, 0xff)
  end

  describe 'with index' do

    it 'should return return a single byte value if in range' do
      @byte_array[0].should == 0xff
    end

    it 'should raise an error if out of range (negative indices do not wrap around)' do
      lambda { @byte_array[-1] }.should raise_error(IndexError)
      lambda { @byte_array[@byte_array.length + 1] }.should raise_error(IndexError)
    end

  end

  describe 'with index and length' do

    it 'should return a newly allocated Rubinius::ByteArray' do
      @byte_array.size.times { |i| @byte_array[i] = i }

      new_byte_array = @byte_array[1, 3]
      new_byte_array.size.should == 3
      new_byte_array[0].should == 0x01
      new_byte_array[1].should == 0x02
      new_byte_array[2].should == 0x03
    end

    it 'should raise an error if out of range (negative indices do not wrap around)' do
      lambda { @byte_array[-1, 3] }.should raise_error(IndexError)
      lambda { @byte_array[@byte_array.length + 1, 3] }.should raise_error(IndexError)
    end

  end

end