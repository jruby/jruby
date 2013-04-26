require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/eql', __FILE__)
require File.expand_path('../shared/equal_value', __FILE__)

ruby_version_is "1.9" do
  describe "String#===" do
    it_behaves_like(:string_eql_value, :===)
    it_behaves_like(:string_equal_value, :===)
  end
end