describe "Lexer" do
  it "should parse identifier at the end of stream" do
    $KCODE, backup = "s", $KCODE if RUBY_VERSION < '1.9'
    begin
      eval("$a").should == nil
    ensure
      $KCODE = backup if RUBY_VERSION < '1.9'
    end
  end
end
