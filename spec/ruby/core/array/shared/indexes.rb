describe :array_indexes, :shared => true do
  it "returns elements at integer argument indexes (DEPRECATED)" do
    array = [1, 2, 3, 4, 5]

    x = mock('4')
    def x.to_int() 4 end

    params = [1, 0, 5, -1, -8, 10, x]
    array.send(@method, *params).should == array.values_at(*params)
  end

  it "tries to convert the passed arguments to Integers using #to_int" do
    obj = mock('to_int')
    obj.should_receive(:to_int).and_return(1, 3)
    [1, 2, 3, 4, 5].send(@method, obj, obj).should == [2, 4]
  end

  it "returns elements in range arguments as nested arrays (DEPRECATED)" do
    array = [1, 2, 3, 4, 5]
    params = [0..2, 1...3, 4..6]
    array.indexes(*params).should == [[1, 2, 3], [2, 3], [5]]
    array.indices(*params).should == [[1, 2, 3], [2, 3], [5]]
  end
end
