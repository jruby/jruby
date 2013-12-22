# Regression test for https://github.com/jruby/jruby/issues/1348 :
# Cannot make two symbols with same bytes and different encodings

describe "symbol table" do
  it "allows different symbols to have the same bytes and different encodings" do
    sym1 = "d8a1af43".force_encoding("UTF-16").to_sym
    sym2 = "d8a1af43".to_sym
    expect(sym1.encoding).to eq Encoding.find("UTF-16")
    expect(sym2.encoding).to eq Encoding.find("US-ASCII")
    expect(sym1).to_not eq sym2
  end
end if defined?(Encoding)