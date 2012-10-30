require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/collect_concat', __FILE__)

ruby_version_is "1.9" do
  describe "Enumerable#flat_map" do
    it_behaves_like(:enumerable_collect_concat , :flat_map)
  end
end
