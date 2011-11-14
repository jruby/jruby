describe "JRUBY-6209: Hash#rehash does not work under some condition" do
  it "works even it contains a key where #hash is negative" do
    a = "ASCII"
    b = "\xE3\x81\x82" # Japanese 'a' in UTF-8, but encoding is not related
    # b.hash < 0 at this moment, but there's no reason to be negative in the future.
    # b.hash.should < 0
    key = [a]
    hash = Hash[key => 100]
    key[0] = b
    hash.rehash
    hash[key].should == 100
  end
end
