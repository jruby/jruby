describe "The unless expression" do
  it "allows expression and body to be on one line (using ':')" do
    unless false: 'foo'; else 'bar'; end.should == 'foo'
  end
end
