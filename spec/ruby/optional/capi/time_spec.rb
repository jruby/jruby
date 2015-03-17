require File.expand_path('../spec_helper', __FILE__)

load_extension("time")

describe "CApiTimeSpecs" do
  before :each do
    @s = CApiTimeSpecs.new
  end

  describe "rb_time_new" do
    it "creates a Time from the sec and usec" do
      usec = CAPI_SIZEOF_LONG == 8 ? 4611686018427387903 : 1413123123
      @s.rb_time_new(1232141421, usec).should == Time.at(1232141421, usec)
    end
  end

  describe "TIMET2NUM" do
    it "returns an Integer" do
      @s.TIMET2NUM.should be_kind_of(Integer)
    end
  end

  describe "rb_time_nano_new" do
    it "creates a Time from the sec and usec" do
      time = @s.rb_time_nano_new(1232141421, 1413123123)
      time.to_i.should == 1232141422
      time.nsec.should == 413123123
    end
  end

  describe "rb_time_num_new" do
    it "creates a Time in the local zone with only a timestamp" do
      with_timezone("Europe/Amsterdam") do
        time = @s.rb_time_num_new(1232141421, nil)
        time.should be_an_instance_of(Time)
        time.to_i.should == 1232141421
        time.gmt_offset.should == 3600
      end
    end

    it "creates a Time with the given offset" do
      with_timezone("Europe/Amsterdam") do
        time = @s.rb_time_num_new(1232141421, 7200)
        time.should be_an_instance_of(Time)
        time.to_i.should == 1232141421
        time.gmt_offset.should == 7200
      end
    end

    it "creates a Time with a Float timestamp" do
      with_timezone("Europe/Amsterdam") do
        time = @s.rb_time_num_new(1.5, 7200)
        time.should be_an_instance_of(Time)
        time.to_i.should == 1
        time.nsec.should == 500000000
        time.gmt_offset.should == 7200
      end
    end

    it "creates a Time with a Rational timestamp" do
      with_timezone("Europe/Amsterdam") do
        time = @s.rb_time_num_new(Rational(3, 2), 7200)
        time.should be_an_instance_of(Time)
        time.to_i.should == 1
        time.nsec.should == 500000000
        time.gmt_offset.should == 7200
      end
    end
  end

  describe "rb_time_interval" do
    it "creates a timeval interval for a Fixnum" do
      sec, usec = @s.rb_time_interval(1232141421)
      sec.should be_kind_of(Integer)
      sec.should == 1232141421
      usec.should be_kind_of(Integer)
      usec.should == 0
    end

    it "creates a timeval interval for a Float" do
      sec, usec = @s.rb_time_interval(1.5)
      sec.should be_kind_of(Integer)
      sec.should == 1
      usec.should be_kind_of(Integer)
      usec.should == 500000
    end

    it "creates a timeval interval for a Rational" do
      sec, usec = @s.rb_time_interval(Rational(3, 2))
      sec.should be_kind_of(Integer)
      sec.should == 1
      usec.should be_kind_of(Integer)
      usec.should == 500000
    end

    it "throws an argument error for a negative value" do
      lambda { @s.rb_time_interval(-1232141421) }.should raise_error(ArgumentError)
      lambda { @s.rb_time_interval(Rational(-3, 2)) }.should raise_error(ArgumentError)
      lambda { @s.rb_time_interval(-1.5) }.should raise_error(ArgumentError)
    end

  end

  describe "rb_time_interval" do
    it "creates a timeval interval for a Fixnum" do
      sec, usec = @s.rb_time_interval(1232141421)
      sec.should be_kind_of(Integer)
      sec.should == 1232141421
      usec.should be_kind_of(Integer)
      usec.should == 0
    end

    it "creates a timeval interval for a Float" do
      sec, usec = @s.rb_time_interval(1.5)
      sec.should be_kind_of(Integer)
      sec.should == 1
      usec.should be_kind_of(Integer)
      usec.should == 500000
    end

    it "creates a timeval interval for a Rational" do
      sec, usec = @s.rb_time_interval(Rational(3, 2))
      sec.should be_kind_of(Integer)
      sec.should == 1
      usec.should be_kind_of(Integer)
      usec.should == 500000
    end

    it "throws an argument error for a negative value" do
      lambda { @s.rb_time_interval(-1232141421) }.should raise_error(ArgumentError)
      lambda { @s.rb_time_interval(Rational(-3, 2)) }.should raise_error(ArgumentError)
      lambda { @s.rb_time_interval(-1.5) }.should raise_error(ArgumentError)
    end

    it "throws an argument error when given a Time instance" do
      lambda { @s.rb_time_interval(Time.now) }.should raise_error(TypeError)
    end

  end

  describe "rb_time_timeval" do
    it "creates a timeval for a Fixnum" do
      sec, usec = @s.rb_time_timeval(1232141421)
      sec.should be_kind_of(Integer)
      sec.should == 1232141421
      usec.should be_kind_of(Integer)
      usec.should == 0
    end

    it "creates a timeval for a Float" do
      sec, usec = @s.rb_time_timeval(1.5)
      sec.should be_kind_of(Integer)
      sec.should == 1
      usec.should be_kind_of(Integer)
      usec.should == 500000
    end

    it "creates a timeval for a Rational" do
      sec, usec = @s.rb_time_timeval(Rational(3, 2))
      sec.should be_kind_of(Integer)
      sec.should == 1
      usec.should be_kind_of(Integer)
      usec.should == 500000
    end

    it "creates a timeval for a negative Fixnum" do
      sec, usec = @s.rb_time_timeval(-1232141421)
      sec.should be_kind_of(Integer)
      sec.should == -1232141421
      usec.should be_kind_of(Integer)
      usec.should == 0
    end

    it "creates a timeval for a negative Float" do
      sec, usec = @s.rb_time_timeval(-1.5)
      sec.should be_kind_of(Integer)
      sec.should == -2
      usec.should be_kind_of(Integer)
      usec.should == 500000
    end

    it "creates a timeval for a negative Rational" do
      sec, usec = @s.rb_time_timeval(Rational(-3, 2))
      sec.should be_kind_of(Integer)
      sec.should == -2
      usec.should be_kind_of(Integer)
      usec.should == 500000
    end

    it "creates a timeval from a Time object" do
      t = Time.now
      sec, usec = @s.rb_time_timeval(t)
      sec.should == t.to_i
      usec.should == t.nsec.div(1000)
    end
  end

  describe "rb_time_timespec" do
    it "creates a timespec for a Fixnum" do
      sec, nsec = @s.rb_time_timespec(1232141421)
      sec.should be_kind_of(Integer)
      sec.should == 1232141421
      nsec.should be_kind_of(Integer)
      nsec.should == 0
    end

    it "creates a timespec for a Float" do
      sec, nsec = @s.rb_time_timespec(1.5)
      sec.should be_kind_of(Integer)
      sec.should == 1
      nsec.should be_kind_of(Integer)
      nsec.should == 500000000
    end

    it "creates a timespec for a Rational" do
      sec, nsec = @s.rb_time_timespec(Rational(3, 2))
      sec.should be_kind_of(Integer)
      sec.should == 1
      nsec.should be_kind_of(Integer)
      nsec.should == 500000000
    end

    it "creates a timespec for a negative Fixnum" do
      sec, nsec = @s.rb_time_timespec(-1232141421)
      sec.should be_kind_of(Integer)
      sec.should == -1232141421
      nsec.should be_kind_of(Integer)
      nsec.should == 0
    end

    it "creates a timespec for a negative Float" do
      sec, nsec = @s.rb_time_timespec(-1.5)
      sec.should be_kind_of(Integer)
      sec.should == -2
      nsec.should be_kind_of(Integer)
      nsec.should == 500000000
    end

    it "creates a timespec for a negative Rational" do
      sec, nsec = @s.rb_time_timespec(Rational(-3, 2))
      sec.should be_kind_of(Integer)
      sec.should == -2
      nsec.should be_kind_of(Integer)
      nsec.should == 500000000
    end

    it "creates a timespec from a Time object" do
      t = Time.now
      sec, nsec = @s.rb_time_timespec(t)
      sec.should == t.to_i
      nsec.should == t.nsec
    end
  end
end
