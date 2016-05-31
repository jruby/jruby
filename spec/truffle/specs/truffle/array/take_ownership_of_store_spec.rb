# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1
# OTHER DEALINGS IN THE SOFTWARE.

require_relative '../../../../ruby/spec_helper'

describe "Truffle::Array.take_ownership_of_store" do
  def storage(ary)
    Truffle::Debug.array_storage(ary)
  end

  before :each do
    @array = %i[first second third]
  end

  it "should no-op when called on itself" do
    copy = @array.dup

    Truffle::Array.take_ownership_of_store(@array, @array)

    storage(@array).should == "Object[]"
    @array.should == copy
  end

  it "should take ownership of the store" do
    other = [1, 2, 3, 4, 5]
    other_copy = other.dup

    Truffle::Array.take_ownership_of_store(@array, other)

    storage(@array).should == "int[]"
    @array.should == other_copy

    storage(other).should == "null"
    other.empty?.should == true
  end
end
