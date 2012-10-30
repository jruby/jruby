require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/each_line', __FILE__)

ruby_version_is "1.8.7" do
  describe "String#lines" do
    it_behaves_like(:string_each_line, :lines)
  end
end
