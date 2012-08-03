require 'rspec'

describe "Time.utc" do
  describe "when given usec" do
    it "Rounds towards zero" do
      target = Time.utc(2012,7,31,23,59,59,999999.999)
      target.month.should == 7
      target.day.should == 31
    end
  end
end
