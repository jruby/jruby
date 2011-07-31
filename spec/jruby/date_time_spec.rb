require 'date'

describe "DateTime" do
  before { @time = DateTime.now }
  context "when @time is initialized" do
    it "should be type of DateTime" do
      @time.should be_a_kind_of DateTime
    end
    
    it "should be instance of DateTime" do
      @time.should be_instance_of DateTime
    end
  end
  
  describe "#to_s" do
    before { @string = @time.to_s }
    it "should be type of String" do
      @string.should be_a_kind_of String
    end
    
    it "should be instance of String" do
      @string.should be_instance_of String
    end
  end
  
  describe "Comparison operators" do
    before {
      @early = DateTime.now
      sleep(1)
      @late = DateTime.now
    }
    context "when @early is less than @late" do
      it "returns true" do
        (@early < @late).should be_true
      end
      
      it "returns -1" do
        (@early <=> @late).should == -1
      end
    end
    
    context "when @early is less than or equal to @late" do
      it "returns true" do
        (@early <= @late).should be_true
      end
    end
    
    context "when @late is greater than @early" do
      it "returns true" do
        (@late > @early).should be_true
      end
      
      it "returns 1" do
        (@late <=> @early).should == 1
      end
    end
    
    context "when @late is greater than or equal to @early" do
      it "returns true" do
        (@late >= @early).should be_true
      end
    end
    
    context "when @early is not equal to @late" do
      it "returns true" do
        (@early != @late).should be_true
      end
    end
    
    context "when @early is equal to @early" do
      it "returns 0" do
        (@early <=> @early).should == 0
      end
    end
  end
end

describe "Time#gm" do
  it "returns time instance with year,month,day,hour,min,sec_with_frac" do
    Time.gm(2007,8,28,0,37,29).to_s.should == "Tue Aug 28 00:37:29 UTC 2007"
  end
  
  it "returns time instance with year,month,day,hour,min" do
    Time.gm(2007,8,28,0,37).to_s.should == "Tue Aug 28 00:37:00 UTC 2007"
  end
  
  it "returns time instance with year,month,day,hour" do
    Time.gm(2007,8,28,0).to_s.should == "Tue Aug 28 00:00:00 UTC 2007"
  end
  
  it "returns time instance with year,month,day" do
    Time.gm(2007,8,28).to_s.should == "Tue Aug 28 00:00:00 UTC 2007"
  end
  
  it "returns time instance with year,month" do
    Time.gm(2007,8).to_s.should == "Wed Aug 01 00:00:00 UTC 2007"
  end
  
  it "returns time instance with year" do
    Time.gm(2007).to_s.should == "Mon Jan 01 00:00:00 UTC 2007"
  end
  
  context "when computation is performed on Time instance" do
    before {
      @t = Time.gm(2005,1,1,0,1,0,10)
      class << @t
        def seconds_since_midnight
          self.to_i - self.change(:hour => 0).to_i + (self.usec/1.0e+6)
        end

        def change(options)
          ::Time.send(self.utc? ? :utc : :local, 
              options[:year]  || self.year, 
              options[:month] || self.month, 
              options[:mday]  || self.mday, 
              options[:hour]  || self.hour, 
              options[:min]   || (options[:hour] ? 0 : self.min),
              options[:sec]   || ((options[:hour] || options[:min]) ? 0 : self.sec),
              options[:usec]  || ((options[:hour] || options[:min] || options[:sec]) ? 0 : self.usec))
        end
      end
    }
    
    it "should return correct value" do
     @t.seconds_since_midnight.should == 60.00001 
    end
  end
end