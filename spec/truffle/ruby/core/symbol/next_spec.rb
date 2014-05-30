require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/succ', __FILE__)

ruby_version_is "1.9" do
  describe "Symbol#next" do
    it_behaves_like :symbol_succ, :next
  end
end