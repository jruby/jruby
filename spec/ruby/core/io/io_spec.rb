require File.expand_path('../../../spec_helper', __FILE__)

describe "IO" do
  it "includes File::Constants" do
    IO.should include(File::Constants)
  end

  it "includes Enumerable" do
    IO.should include(Enumerable)
  end
end
