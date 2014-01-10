require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/rect', __FILE__)

ruby_version_is "1.9" do
  describe "Numeric#rectangular" do
    it_behaves_like(:numeric_rect, :rectangular)
  end
end
