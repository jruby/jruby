require File.expand_path('../spec_helper', __FILE__)

describe "Managed Struct" do
  it "raises an error if release() is not defined" do
    lambda {
      FFISpecs::NoRelease.new(FFISpecs::LibTest.ptr_from_address(0x12345678))
    }.should raise_error(NoMethodError)
  end

  it "is the right class" do
    ptr = FFISpecs::WhatClassAmI.new(FFISpecs::LibTest.ptr_from_address(0x12345678))
    ptr.should be_kind_of(FFISpecs::WhatClassAmI)
  end

  it "releases memory properly" do
    loop_count = 30
    wiggle_room = 2

    FFISpecs::PleaseReleaseMe.should_receive(:release).at_least(loop_count - wiggle_room).times
    loop_count.times do
      s = FFISpecs::PleaseReleaseMe.new(FFISpecs::LibTest.ptr_from_address(0x12345678))
    end

    FFISpecs::PleaseReleaseMe.wait_gc loop_count
  end
end
