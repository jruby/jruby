describe "A Symbol literal" do
  it "must not be an empty string" do
    lambda { eval ":''" }.should raise_error(SyntaxError)
  end
end
