describe "Lexer" do
  it "should parse identifier at the end of stream" do
    expect(eval("$a")).to eq(nil)
  end
end
