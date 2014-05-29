require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/object/object_id', __FILE__)

ruby_version_is "".."1.9" do
  describe "Object#__id__" do
    it_behaves_like :basic_object_id, :__id__, Object
    it_behaves_like :object_id,       :__id__, Object
  end
end
