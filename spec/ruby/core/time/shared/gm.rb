describe :time_gm, :shared => true do
  ruby_version_is ""..."1.9" do
    it "creates a time based on given values, interpreted as UTC (GMT)" do
      Time.send(@method, 2000,"jan",1,20,15,1).inspect.should == "Sat Jan 01 20:15:01 UTC 2000"
    end

    it "creates a time based on given C-style gmtime arguments, interpreted as UTC (GMT)" do
      time = Time.send(@method, 1, 15, 20, 1, 1, 2000, :ignored, :ignored, :ignored, :ignored)
      time.inspect.should == "Sat Jan 01 20:15:01 UTC 2000"
    end
  end

  ruby_version_is "1.9" do
    it "creates a time based on given values, interpreted as UTC (GMT)" do
      Time.send(@method, 2000,"jan",1,20,15,1).inspect.should == "2000-01-01 20:15:01 UTC"
    end

    it "creates a time based on given C-style gmtime arguments, interpreted as UTC (GMT)" do
      time = Time.send(@method, 1, 15, 20, 1, 1, 2000, :ignored, :ignored, :ignored, :ignored)
      time.inspect.should == "2000-01-01 20:15:01 UTC"
    end
  end

  it "allows leap seconds like MRI does" do
    # From MRI code: https://github.com/ruby/ruby/blob/trunk/time.c#L2609
    next_day = Time.send(@method, 2000, 1, 2)

    Time.send(@method, 2000, 1, 1, 24).should == next_day
    Time.send(@method, 2000, 1, 1, 23, 59, 60).should == next_day
  end

  it "validates time like MRI does" do
    [
      [ 2000, 1, 1, 24, 1 ],
      [ 2000, 1, 1, 23, 59, 61 ],
      [ 2000, 1, 1, 25, 0, 0 ]
    ].each do |bad_args|
      proc { Time.send(@method, *bad_args) }.should raise_error(
        ArgumentError, /out of range/)
    end
  end
end
