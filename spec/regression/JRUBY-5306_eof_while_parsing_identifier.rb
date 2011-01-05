describe "Lexer" do
  it "should parse identifier at the end of stream" do
    $KCODE, backup = "s", $KCODE
    begin
      eval("$a").should == nil
    ensure
      $KCODE = backup
    end
  end
end
