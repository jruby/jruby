require 'spec_helper'
require 'mspec/guards'
require 'mspec/helpers'

describe "#enumerator_class" do
  it "returns Enumerator in Ruby 1.8.7+" do
    MSpec.stub :deprecate
    enumerator_class.should == Enumerator
  end
end
