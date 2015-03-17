require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/concat', __FILE__)

describe "String#concat" do
  it_behaves_like :string_concat, :concat
  it_behaves_like :string_concat_encoding, :concat
end
