require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/include', __FILE__)

describe "Range#===" do
  ruby_version_is ""..."1.9" do
    it_behaves_like(:range_include, :===)
  end

  ruby_version_is "1.9" do
    it "returns the result of calling #include? on self" do
      range = 0...10
      range.should_receive(:include?).with(2).and_return(:true)
      (range === 2).should == :true
    end
  end
end
