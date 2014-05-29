require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/each_line', __FILE__)
require File.expand_path('../shared/each_line_without_block', __FILE__)

ruby_version_is "1.8.7" do
  describe "String#lines" do
    it_behaves_like(:string_each_line, :lines)

    ruby_version_is ""..."2.0" do
      it_behaves_like(:string_each_line_without_block, :each_line)
    end

    ruby_version_is "2.0" do
      it "returns an array when no block given" do
        ary = "hello world".send(@method, ' ')
        ary.should == ["hello ", "world"]
      end
    end
  end
end
