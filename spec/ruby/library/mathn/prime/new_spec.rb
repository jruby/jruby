require File.expand_path('../../../../spec_helper', __FILE__)
require 'mathn'

describe "Prime.new" do
  it "returns a new Prime number" do
    Prime.new.should be_kind_of(Prime)
  end

  it "raises a TypeError when is called with some arguments" do
    lambda { Prime.new(1) }.should raise_error(ArgumentError)
  end
end
