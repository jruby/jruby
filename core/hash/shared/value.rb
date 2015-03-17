describe :hash_value_p, :shared => true do
  it "returns true if the value exists in the hash" do
    new_hash(:a => :b).send(@method, :a).should == false
    new_hash(1 => 2).send(@method, 2).should == true
    h = new_hash 5
    h.send(@method, 5).should == false
    h = new_hash { 5 }
    h.send(@method, 5).should == false
  end

  it "uses == semantics for comparing values" do
    new_hash(5 => 2.0).send(@method, 2).should == true
  end
end
