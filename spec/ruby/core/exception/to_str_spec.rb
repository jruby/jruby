require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/to_s', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Exception#to_str" do
    it_behaves_like :to_s, :to_str
  end
end

ruby_version_is "1.9" do
  describe "Exception#to_str" do
    it "has been deprecated" do
      Exception.new.should_not respond_to(:to_str)
    end
  end
end

