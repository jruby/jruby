require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/object/object_id', __FILE__)

describe "Object#object_id" do
  it_behaves_like :basic_object_id, :object_id, Object
  it_behaves_like :object_id,       :object_id, Object
end
