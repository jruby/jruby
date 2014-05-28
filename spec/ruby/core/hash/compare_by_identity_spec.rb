describe "Hash#compare_by_identity" do
  before(:each) do
    @h = new_hash
    @idh = new_hash.compare_by_identity
  end

  it "causes future comparisons on the receiver to be made by identity" do
    @h["a"] = :a
    @h["a"].should == :a
    @h.compare_by_identity
    @h["a"].should be_nil
  end

  it "causes #compare_by_identity? to return true" do
    @idh.compare_by_identity?.should be_true
  end

  it "returns self" do
    h = new_hash
    h[:foo] = :bar
    h.compare_by_identity.should == h
  end

  it "uses the semantics of BasicObject#equal? to determine key identity" do
    [1].should_not equal([1])
    @idh[[1]] = :c
    @idh[[1]] = :d
    :bar.should equal(:bar)
    @idh[:bar] = :e
    @idh[:bar] = :f
    "bar".should_not equal('bar')
    @idh["bar"] = :g
    @idh["bar"] = :h
    @idh.values.should == [:c, :d, :f, :g, :h]
  end

  it "uses #equal? semantics, but doesn't actually call #equal? to determine identity" do
    obj = mock('equal')
    obj.should_not_receive(:equal?)
    @idh[:foo] = :glark
    @idh[obj] = :a
    @idh[obj].should == :a
  end

  it "regards #dup'd objects as having different identities" do
    str = 'foo'
    @idh[str.dup] = :str
    @idh[str].should be_nil
  end

  it "regards #clone'd objects as having different identities" do
    str = 'foo'
    @idh[str.clone] = :str
    @idh[str].should be_nil
  end

  it "regards references to the same object as having the same identity" do
    o = Object.new
    @h[o] = :o
    @h[:a] = :a
    @h[o].should == :o
  end

  it "raises a RuntimeError on frozen hashes" do
    @h = @h.freeze
    lambda { @h.compare_by_identity }.should raise_error(RuntimeError)
  end

  # Behaviour confirmed in bug #1871
  it "perists over #dups" do
    @idh['foo'] = :bar
    @idh['foo'] = :glark
    @idh.dup.should == @idh
    @idh.dup.size.should == @idh.size
  end

  it "persists over #clones" do
    @idh['foo'] = :bar
    @idh['foo'] = :glark
    @idh.clone.should == @idh
    @idh.clone.size.should == @idh.size
  end

  it "does not copy string keys" do
    foo = 'foo'
    @idh[foo] = true
    @idh[foo] = true
    @idh.size.should == 1
    @idh.keys.first.object_id.should == foo.object_id
  end
end

describe "Hash#compare_by_identity?" do
  it "returns false by default" do
    h = new_hash
    h.compare_by_identity?.should be_false
  end

  it "returns true once #compare_by_identity has been invoked on self" do
    h = new_hash
    h.compare_by_identity
    h.compare_by_identity?.should be_true
  end

  it "returns true when called multiple times on the same ident hash" do
    h = new_hash
    h.compare_by_identity
    h.compare_by_identity?.should be_true
    h.compare_by_identity?.should be_true
    h.compare_by_identity?.should be_true
  end
end
