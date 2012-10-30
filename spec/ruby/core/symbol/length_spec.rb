require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  require File.expand_path('../shared/length', __FILE__)

  describe "Symbol#length" do
    it_behaves_like :symbol_length, :length
  end
end
