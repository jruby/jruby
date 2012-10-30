require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/codepoints', __FILE__)

with_feature :encoding do
  describe "String#each_codepoint" do
    it_behaves_like(:string_codepoints, :each_codepoint)
  end
end
