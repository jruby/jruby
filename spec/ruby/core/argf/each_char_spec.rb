require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/each_char', __FILE__)

ruby_version_is "1.8.7" do
  describe "ARGF.each_char" do
    it_behaves_like :argf_each_char, :each_char
  end
end
