describe "Lexer" do
  it "should parse identifier at the end of stream" do
    eval("$a").should == nil
  end
end
