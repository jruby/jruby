require File.expand_path('../shared/chars', __FILE__)

ruby_version_is '1.8.7' do
  describe "String#chars" do
    it_behaves_like(:string_chars, :chars)
  end
end
