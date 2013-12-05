require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/methods', __FILE__)

describe "Time.at" do
  describe "passed Numeric" do
    it "returns a Time object representing the given number of Integer seconds since 1970-01-01 00:00:00 UTC" do
      Time.at(1184027924).getgm.asctime.should == "Tue Jul 10 00:38:44 2007"
    end

    it "returns a Time object representing the given number of Float seconds since 1970-01-01 00:00:00 UTC" do
      t = Time.at(10.5)
      t.usec.should == 500000.0
      t.should_not == Time.at(10)
    end

    it "returns a non-UTC Time" do
      Time.at(1184027924).utc?.should == false
    end

    it "returns a subclass instance on a Time subclass" do
      c = Class.new(Time)
      t = c.at(0)
      t.should be_kind_of(c)
    end
  end

  describe "passed Time" do
    it "creates a new time object with the value given by time" do
      t = Time.now
      Time.at(t).inspect.should == t.inspect
    end

    it "creates a dup time object with the value given by time" do
      t1 = Time.new
      t2 = Time.at(t1)
      t1.object_id.should_not == t2.object_id
    end

    it "returns a UTC time if the argument is UTC" do
      t = Time.now.getgm
      Time.at(t).utc?.should == true
    end

    it "returns a non-UTC time if the argument is non-UTC" do
      t = Time.now
      Time.at(t).utc?.should == false
    end

    it "returns a subclass instance" do
      c = Class.new(Time)
      t = c.at(Time.now)
      t.should be_kind_of(c)
    end
  end

  describe "passed non-Time, non-Numeric" do
    it "raises a TypeError with a String argument" do
      lambda { Time.at("0") }.should raise_error(TypeError)
    end

    it "raises a TypeError with a nil argument" do
      lambda { Time.at(nil) }.should raise_error(TypeError)
    end

    describe "with an argument that responds to #to_int" do
      ruby_version_is ""..."1.9" do
        it "raises a TypeError" do
          o = mock('integer')
          o.should_not_receive(:to_int)
          lambda { Time.at(o) }.should raise_error(TypeError)
        end
      end

      ruby_version_is "1.9" do
        it "coerces using #to_int" do
          o = mock('integer')
          o.should_receive(:to_int).and_return(0)
          Time.at(o).should == Time.at(0)
        end
      end
    end

    describe "with an argument that responds to #to_r" do
      ruby_version_is ""..."1.9" do
        it "raises a TypeError" do
          o = mock_numeric('rational')
          o.should_not_receive(:to_r)
          lambda { Time.at(o) }.should raise_error(TypeError)
        end
      end

      ruby_version_is "1.9" do
        it "coerces using #to_r" do
          o = mock_numeric('rational')
          o.should_receive(:to_r).and_return(Rational(5, 2))
          Time.at(o).should == Time.at(Rational(5, 2))
        end
      end
    end
  end

  describe "passed [Integer, Numeric]" do
    it "returns a Time object representing the given number of seconds and Integer microseconds since 1970-01-01 00:00:00 UTC" do
      t = Time.at(10, 500000)
      t.tv_sec.should == 10
      t.tv_usec.should == 500000
    end

    ruby_version_is "1.9" do
      it "returns a Time object representing the given number of seconds and Float microseconds since 1970-01-01 00:00:00 UTC" do
        t = Time.at(10, 500.500)
        t.tv_sec.should == 10
        t.tv_nsec.should == 500500
      end
    end
  end

  describe "with a second argument that responds to #to_int" do
    it "coerces using #to_int" do
      o = mock('integer')
      o.should_receive(:to_int).and_return(10)
      Time.at(0, o).should == Time.at(0, 10)
    end
  end

  describe "with a second argument that responds to #to_r" do
    ruby_version_is ""..."1.9" do
      it "raises a TypeError" do
        o = mock('rational')
        o.should_not_receive(:to_r)
        lambda { Time.at(0, o) }.should raise_error(TypeError)
      end
    end

    ruby_version_is "1.9" do
      it "coerces using #to_r" do
        o = mock_numeric('rational')
        o.should_receive(:to_r).and_return(Rational(5, 2))
        Time.at(0, o).should == Time.at(0, Rational(5, 2))
      end
    end
  end

  describe "passed [Integer, nil]" do
    it "raises a TypeError" do
      lambda { Time.at(0, nil) }.should raise_error(TypeError)
    end
  end

  describe "passed [Integer, String]" do
    it "raises a TypeError" do
      lambda { Time.at(0, "0") }.should raise_error(TypeError)
    end
  end

  describe "passed [Time, Integer]" do
    ruby_version_is ""..."1.9" do
      it "raises a TypeError" do
        lambda { Time.at(Time.now, 500000) }.should raise_error(TypeError)
      end
    end

    ruby_bug "#8173", "2.0" do
      it "raises a TypeError" do
        lambda { Time.at(Time.now, 500000) }.should raise_error(TypeError)
      end
    end
  end
end
