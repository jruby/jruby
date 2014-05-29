require File.expand_path('../../../spec_helper', __FILE__)

describe "Symbol.all_symbols" do
  it "returns an array containing all the Symbols in the symbol table" do
    Symbol.all_symbols.is_a?(Array).should == true
    Symbol.all_symbols.all? { |s| s.is_a?(Symbol) ? true : (p s; false) }.should == true
  end

  it "increases size of the return array when new symbol comes" do
    num_symbols = Symbol.all_symbols.size
    eval ":aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    Symbol.all_symbols.size.should == num_symbols + 1
  end
end
