require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/to_a', __FILE__)

ruby_version_is ""..."1.9" do
  describe "String#to_a" do
    it_behaves_like :string_to_a, :to_a
  end
end
