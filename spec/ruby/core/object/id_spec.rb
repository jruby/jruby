require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/object/object_id', __FILE__)

ruby_version_is "".."1.9" do
  describe "Object#id" do
    it_behaves_like :basic_object_id, :id, Object
    it_behaves_like :object_id,       :id, Object
  end
end
