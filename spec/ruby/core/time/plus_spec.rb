require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/methods', __FILE__)

describe "Time#+" do
  it "increments the time by the specified amount" do
    (Time.at(0) + 100).should == Time.at(100)
  end

  it "is a commutative operator" do
    (Time.at(1.1) + 0.9).should == Time.at(0.9) + 1.1
  end

  ruby_version_is "" ... "1.9" do
    it "rounds micro seconds rather than truncates" do
      # The use of 8.9999999 is intentional. This is because
      # Time treats the fractional part as the number of micro seconds.
      # Thusly it multiplies the result by 1_000_000 to go from
      # seconds to microseconds. That conversion should be rounded
      # properly. In this case, it's rounded up to 1,000,000, and thus
      # contributes a full extra second to the Time object.
      t = Time.at(0) + 8.9999999
      t.should == Time.at(9)
      t.usec.should == 0

      # Check the non-edge case works properly, that the fractional part
      # contributes to #usecs
      t2 = Time.at(0) + 8.9
      t2.usec.should == 900000
    end

    it "adds a negative Float" do
      t = Time.at(100) + -1.3
      t.usec.should == 700000
      t.to_i.should == 98
    end
  end

  ruby_version_is "1.9" do
    it "adds a negative Float" do
      t = Time.at(100) + -1.3
      t.usec.should == 699999
      t.to_i.should == 98
    end
  end

  ruby_version_is "" ... "1.9" do
    it "increments the time by the specified amount as float numbers" do
      (Time.at(1.1) + 0.9).should == Time.at(2)
    end

    it "accepts arguments that can be coerced into Float" do
      (obj = mock('10.5')).should_receive(:to_f).and_return(10.5)
      (Time.at(100) + obj).should == Time.at(110.5)
    end

    it "raises TypeError on argument that can't be coerced into Float" do
      lambda { Time.now + Object.new }.should raise_error(TypeError)
      lambda { Time.now + "stuff" }.should raise_error(TypeError)
    end
  end

  ruby_version_is "1.9" do
    it "increments the time by the specified amount as rational numbers" do
      (Time.at(Rational(11, 10)) + Rational(9, 10)).should == Time.at(2)
    end

    it "accepts arguments that can be coerced into Rational" do
      (obj = mock_numeric('10')).should_receive(:to_r).and_return(Rational(10))
      (Time.at(100) + obj).should == Time.at(110)
    end

    it "raises TypeError on argument that can't be coerced into Rational" do
      lambda { Time.now + Object.new }.should raise_error(TypeError)
      lambda { Time.now + "stuff" }.should raise_error(TypeError)
    end
  end

  it "returns a UTC time if self is UTC" do
    (Time.utc(2012) + 10).utc?.should == true
  end

  it "returns a non-UTC time if self is non-UTC" do
    (Time.local(2012) + 10).utc?.should == false
  end

  ruby_version_is "1.9" do
    it "returns a time with the same fixed offset as self" do
      (Time.new(2012, 1, 1, 0, 0, 0, 3600) + 10).utc_offset.should == 3600
    end
  end

  it "does not returns a subclass instance" do
    c = Class.new(Time)
    x = c.now + 1
    x.should be_kind_of(Time)
  end

  it "raises TypeError on Time argument" do
    lambda { Time.now + Time.now }.should raise_error(TypeError)
  end

  it "raises TypeError on nil argument" do
    lambda { Time.now + nil }.should raise_error(TypeError)
  end

  ruby_version_is "1.9" do
    #see [ruby-dev:38446]
    it "tracks microseconds" do
      time = Time.at(0)
      time += Rational(123_456, 1_000_000)
      time.usec.should == 123_456
      time += Rational(654_321, 1_000_000)
      time.usec.should == 777_777
    end

    it "tracks nanoseconds" do
      time = Time.at(0)
      time += Rational(123_456_789, 1_000_000_000)
      time.nsec.should == 123_456_789
      time += Rational(876_543_210, 1_000_000_000)
      time.nsec.should == 999_999_999
    end

    it "maintains precision" do
      t = Time.at(0) + Rational(8_999_999_999_999_999, 1_000_000_000_000_000)
      t.should_not == Time.at(9)
    end

    it "maintains microseconds precision" do
      time = Time.at(0) + Rational(8_999_999_999_999_999, 1_000_000_000_000_000)
      time.usec.should == 999_999
    end

    it "maintains nanoseconds precision" do
      time = Time.at(0) + Rational(8_999_999_999_999_999, 1_000_000_000_000_000)
      time.nsec.should == 999_999_999
    end

    it "maintains subseconds precision" do
      time = Time.at(0) + Rational(8_999_999_999_999_999, 1_000_000_000_000_000)
      time.subsec.should == Rational(999_999_999_999_999, 1_000_000_000_000_000)
    end
  end
end
