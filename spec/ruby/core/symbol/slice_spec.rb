require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/slice.rb', __FILE__)

ruby_version_is "1.9" do
  describe "Symbol#slice" do
    it_behaves_like(:symbol_slice, :slice)
  end
end
