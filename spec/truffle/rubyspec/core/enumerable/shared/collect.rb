describe :enumerable_collect, :shared => true do
  before :each do
    ScratchPad.record []
  end

  it "returns a new array with the results of passing each element to block" do
    entries = [0, 1, 3, 4, 5, 6]
    numerous = EnumerableSpecs::Numerous.new(*entries)
    numerous.send(@method) { |i| i % 2 }.should == [0, 1, 1, 0, 1, 0]
    numerous.send(@method) { |i| i }.should == entries
  end

  ruby_version_is "1.8.7" do
    it "passes through the values yielded by #each_with_index" do
      [:a, :b].each_with_index.send(@method) { |x, i| ScratchPad << [x, i]; nil }
      ScratchPad.recorded.should == [[:a, 0], [:b, 1]]
    end
  end

  ruby_version_is ""..."1.9" do
    it "gathers whole arrays as elements when each yields multiple" do
      multi = EnumerableSpecs::YieldsMulti.new
      multi.send(@method) {|e| e}.should == [[1,2],[3,4,5],[6,7,8,9]]
    end

    it "returns to_a when no block given" do
      EnumerableSpecs::Numerous.new.send(@method).should == [2, 5, 3, 6, 1, 4]
    end
  end

  ruby_version_is "1.9" do
    it "gathers initial args as elements when each yields multiple" do
      multi = EnumerableSpecs::YieldsMulti.new
      multi.send(@method) {|e| e}.should == [1,3,6]
    end

    it "returns an enumerator when no block given" do
      enum = EnumerableSpecs::Numerous.new.send(@method)
      enum.should be_an_instance_of(enumerator_class)
      enum.each { |i| -i }.should == [-2, -5, -3, -6, -1, -4]
    end
  end
end
