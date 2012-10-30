require File.expand_path('../../../../spec_helper', __FILE__)
require 'matrix'

describe "Vector#eql?" do
  before do
    @vector = Vector[1, 2, 3, 4, 5]
  end

  it "returns true for self" do
    @vector.eql?(@vector).should be_true
  end

  ruby_bug("[ruby-dev:36298]", "1.8.7") do
    it "returns false when there are a pair corresponding elements which are not equal in the sense of Object#eql?" do
      @vector.eql?(Vector[1, 2, 3, 4, 5.0]).should be_false
    end
  end
end
