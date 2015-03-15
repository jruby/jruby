require "stringio"

describe "StringIO" do
  it "includes the Enumerable module" do
    StringIO.should include(Enumerable)
  end
end

