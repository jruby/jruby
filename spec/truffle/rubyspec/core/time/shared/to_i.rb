describe :time_to_i, :shared => true do
  it "returns the value of time as an integer number of seconds since epoch" do
    Time.at(0).send(@method).should == 0
  end
end
