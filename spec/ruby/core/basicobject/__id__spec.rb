require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/object/object_id', __FILE__)

ruby_version_is "1.9.3" do
  describe "BasicObject#__id__" do
    it_behaves_like :basic_object_id, :__id__, BasicObject
  end
end
