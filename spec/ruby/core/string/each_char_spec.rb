require File.expand_path('../shared/chars', __FILE__)
require File.expand_path('../shared/each_char_without_block', __FILE__)

ruby_version_is '1.8.7' do
  describe "String#each_char" do
    it_behaves_like(:string_chars, :each_char)
    it_behaves_like(:string_each_char_without_block, :each_char)
  end
end

