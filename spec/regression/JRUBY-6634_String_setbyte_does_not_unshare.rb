require 'rspec'

if RUBY_VERSION > "1.9"
  describe "String#setbyte" do
    it "forces the string to unshare, and does not modify other sharers" do
      s = "test"
      t = String.new(s) # depends on this sharing the backing store
      t.setbyte(0, "r".ord)
      t.should == eval('"rest"') # eval these, so compiler/ast does not reuse ByteList
      s.should == eval('"test"')
    end
  end
end
