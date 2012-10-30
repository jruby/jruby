describe :hash_store, :shared => true do
  it "associates the key with the value and return the value" do
    h = new_hash(:a => 1)
    h.send(@method, :b, 2).should == 2
    h.should == new_hash(:b=>2, :a=>1)
  end

  it "duplicates string keys using dup semantics" do
    # dup doesn't copy singleton methods
    key = "foo"
    def key.reverse() "bar" end
    h = new_hash
    h.send(@method, key, 0)
    h.keys[0].reverse.should == "oof"
  end

  it "stores unequal keys that hash to the same value" do
    h = new_hash
    k1 = ["x"]
    k2 = ["y"]
    # So they end up in the same bucket
    k1.should_receive(:hash).and_return(0)
    k2.should_receive(:hash).and_return(0)

    h[k1] = 1
    h[k2] = 2
    h.size.should == 2
  end

  it "duplicates and freezes string keys" do
    key = "foo"
    h = new_hash
    h.send(@method, key, 0)
    key << "bar"

    h.should == new_hash("foo" => 0)
    h.keys[0].frozen?.should == true
  end

  it "doesn't duplicate and freeze already frozen string keys" do
    key = "foo".freeze
    h = new_hash
    h.send(@method, key, 0)
    h.keys[0].should equal(key)
  end

  ruby_version_is ""..."1.9" do
    it "raises a TypeError if called on a frozen instance" do
      lambda { HashSpecs.frozen_hash.send(@method, 1, 2) }.should raise_error(TypeError)
    end
  end

  ruby_version_is "1.9" do
    it "raises a RuntimeError if called on a frozen instance" do
      lambda { HashSpecs.frozen_hash.send(@method, 1, 2) }.should raise_error(RuntimeError)
    end
  end

  it "does not raise an exception if changing the value of an existing key during iteration" do
      hash = {1 => 2, 3 => 4, 5 => 6}
      hash.each { hash.send(@method, 1, :foo) }
      hash.should == {1 => :foo, 3 => 4, 5 => 6}
  end
end
