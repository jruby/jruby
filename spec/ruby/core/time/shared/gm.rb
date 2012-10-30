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
end
