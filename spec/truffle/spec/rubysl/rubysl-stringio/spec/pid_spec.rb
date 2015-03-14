require File.expand_path('../fixtures/classes', __FILE__)

describe "StringIO#pid" do
  it "returns nil" do
    StringIO.new("pid").pid.should be_nil
  end
end
