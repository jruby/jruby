require File.dirname(__FILE__) + '/../../spec_helper'

context "Execution literal" do
  specify "`` should return the result of the executed sub-process" do
    ip = 'world'
    `echo disc #{ip}`.should == "disc world\n"
  end

  # NOTE: Interpolation ? It's not consistant with %w for example.
  specify "%x() is the same (with also interpolation)" do
    ip = 'world'
    %x(echo disc #{ip}).should == "disc world\n"
  end

  # NOTE: %X doesn't exist.
end
