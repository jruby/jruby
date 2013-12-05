describe "Kernel#__dir__" do
  it "returns the parent dir of the current file" do
    File.dirname(__FILE__).should == __dir__
  end
end if RUBY_VERSION > "2.0"