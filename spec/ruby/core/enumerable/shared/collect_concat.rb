describe :enumerable_collect_concat, :shared => true do
  it "returns a new array with the results of passing each element to block, flattened one level" do
    numerous = EnumerableSpecs::Numerous.new(1, [2, 3], [4, [5, 6]], {:foo => :bar})
    numerous.send(@method){ |i| i }.should == [1, 2, 3, 4, [5, 6], {:foo => :bar}]
  end

  it "skips elements that are empty Arrays" do
    numerous = EnumerableSpecs::Numerous.new(1, [], 2)
    numerous.send(@method){ |i| i }.should == [1, 2]
  end

  it "calls to_ary but not to_a" do
    obj = mock('array-like')
    obj.should_receive(:to_ary).and_return([:foo])
    obj2 = mock('has a to_a')
    obj2.should_not_receive(:to_a)

    numerous = EnumerableSpecs::Numerous.new(obj, obj2)
    numerous.send(@method){ |i| i }.should == [:foo, obj2]
  end

  it "returns an enumerator when no block given" do
    enum = EnumerableSpecs::Numerous.new(1, 2).send(@method)
    enum.should be_an_instance_of(enumerator_class)
    enum.each{ |i| [i] * i }.should == [1, 2, 2]
  end
end
