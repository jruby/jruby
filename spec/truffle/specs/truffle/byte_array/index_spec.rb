# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1
# OTHER DEALINGS IN THE SOFTWARE.

require_relative '../../../../ruby/spec_helper'

describe 'Rubinius::ByteArray#index' do

  before :each do
    @byte_array = Rubinius::ByteArray.new(16, 0xff)
  end

  describe 'with value not in string' do

    it 'should return nil' do
      @byte_array.index(0xbb).should be_nil
    end

  end

  describe 'with index and length' do

    it 'should return the index corresponding to the first occurrence of the value' do

      @byte_array[5] = 0xcc
      @byte_array.index(0xcc).should == 5

      @byte_array[2] = 0xcc
      @byte_array.index(0xcc).should == 2

    end

  end

end