describe :enum_each, :shared => true do
  it "yields each element of self to the given block" do
    acc = []
    enumerator_class.new([1,2,3]).each {|e| acc << e }
    acc.should == [1,2,3]
  end

  it "calls #each on the object given in the constructor by default" do
    each = mock('each')
    each.should_receive(:each)
    enumerator_class.new(each).each {|e| e }
  end

  it "calls #each on the underlying object until it's exhausted" do
    each = mock('each')
    each.should_receive(:each).and_yield(1).and_yield(2).and_yield(3)
    acc = []
    enumerator_class.new(each).each {|e| acc << e }
    acc.should == [1,2,3]
  end

  it "calls the method given in the constructor instead of #each" do
    each = mock('peach')
    each.should_receive(:peach)
    enumerator_class.new(each, :peach).each {|e| e }
  end

  it "calls the method given in the constructor until it's exhausted" do
    each = mock('each')
    each.should_receive(:each).and_yield(1).and_yield(2).and_yield(3)
    acc = []
    enumerator_class.new(each).each {|e| acc << e }
    acc.should == [1,2,3]
  end

  it "raises a NoMethodError if the object doesn't respond to #each" do
    lambda do
      enumerator_class.new(Object.new).each {|e| e }
    end.should raise_error(NoMethodError)
  end

  it "returns an Enumerator if no block is given" do
    enumerator_class.new([1]).each.should be_an_instance_of(enumerator_class)
  end
end
