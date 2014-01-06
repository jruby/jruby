require File.expand_path('../shared/chars', __FILE__)
require File.expand_path('../shared/each_char_without_block', __FILE__)

ruby_version_is '1.8.7' do
  describe "String#chars" do
    it_behaves_like(:string_chars, :chars)

    ruby_version_is ""..."2.0" do
      it_behaves_like(:string_each_char_without_block, :chars)
    end

    ruby_version_is "2.0" do
      it "returns an array when no block given" do
        ary = "hello".send(@method)
        ary.should == ['h', 'e', 'l', 'l', 'o']
      end
    end
  end
end
