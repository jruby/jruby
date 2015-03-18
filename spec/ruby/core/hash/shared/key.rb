describe :hash_key_p, :shared => true do
  it "returns true if argument is a key" do
    h = new_hash(:a => 1, :b => 2, :c => 3, 4 => 0)
    h.send(@method, :a).should == true
    h.send(@method, :b).should == true
    h.send(@method, 2).should == false
    h.send(@method, 4).should == true

    not_compliant_on :opal do
      h.send(@method, 'b').should == false
      h.send(@method, 4.0).should == false
    end
  end

  it "returns true if the key's matching value was nil" do
    new_hash(:xyz => nil).send(@method, :xyz).should == true
  end

  it "returns true if the key's matching value was false" do
    new_hash(:xyz => false).send(@method, :xyz).should == true
  end

  it "returns true if the key is nil" do
    new_hash(nil => 'b').send(@method, nil).should == true
    new_hash(nil => nil).send(@method, nil).should == true
  end

  it "compares keys with the same #hash value via #eql?" do
    x = mock('x')
    x.stub!(:hash).and_return(42)

    y = mock('y')
    y.stub!(:hash).and_return(42)
    y.should_receive(:eql?).and_return(false)

    new_hash(x => nil).send(@method, y).should == false
  end
end
