# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1
# OTHER DEALINGS IN THE SOFTWARE.

require_relative '../../../../ruby/spec_helper'

describe 'Rubinius::ByteArray#chomp!' do

  before :each do
    @byte_array = Rubinius::ByteArray.new(16, 0xff)
  end

  describe 'with value present at end of byte array' do

    it 'should truncate the portion of the byte array after the found byte' do
      original_size = @byte_array.size

      @byte_array.chomp!(0xff).should_not be_nil
      @byte_array.size.should == original_size - 1
    end

  end

  describe 'viwth value present, but not at end of byte array' do

    it 'should truncate the portion of the byte array after the found byte' do
      @byte_array[@byte_array.size - 1] = 0xcc
      original_size = @byte_array.size

      @byte_array.chomp!(0xff).should be_nil
      @byte_array.size.should == original_size
    end

  end

end