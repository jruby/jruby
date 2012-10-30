require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/to_s', __FILE__)

describe "Exception#to_s" do
  it_behaves_like :to_s, :to_s
end

describe "NameError#to_s" do
  it "needs to be reviewed for spec completeness"
end
