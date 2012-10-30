require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Array#choice" do
  ruby_version_is "1.8.7"..."1.9" do
    it "selects a random value from the array" do
      a = [1,2,3,4]
      10.times { a.include?(a.choice).should be_true }
    end

    it "returns nil for empty arrays" do
      [].choice.should be_nil
    end
  end
end
