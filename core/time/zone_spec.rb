require File.expand_path('../../../spec_helper', __FILE__)

describe "Time#zone" do
  it "returns the time zone used for time" do
    # Testing with Asia/Kuwait here because it doesn't have DST.
    with_timezone("Asia/Kuwait") do
      Time.now.zone.should == "AST"
    end
  end

  it "returns nil for a Time with a fixed offset" do
    Time.new(2001, 1, 1, 0, 0, 0, "+05:00").zone.should == nil
  end

  it "returns the correct timezone for a local time" do
    t = Time.new(2005, 2, 27, 22, 50, 0, -3600)

    with_timezone("America/New_York") do
      t.getlocal.zone.should == "EST"
    end
  end

  it "returns nil when getting the local time with a fixed offset" do
    t = Time.new(2005, 2, 27, 22, 50, 0, -3600)

    with_timezone("America/New_York") do
      t.getlocal("+05:00").zone.should be_nil
    end
  end

  describe "Encoding.default_internal is set" do
    before :each do
      @encoding = Encoding.default_internal
      Encoding.default_internal = Encoding::UTF_8
    end

    after :each do
      Encoding.default_internal = @encoding
    end

    ruby_version_is ""..."2.2" do
      it "returns the string with the default internal encoding" do
        t = Time.new(2005, 2, 27, 22, 50, 0, -3600)

        with_timezone("America/New_York") do
          t.getlocal.zone.encoding.should == Encoding::UTF_8
        end
      end
    end

    ruby_version_is "2.2" do
      ruby_bug "#10887", "2.2.0.81" do
        it "returns an ASCII string" do
          t = Time.new(2005, 2, 27, 22, 50, 0, -3600)

          with_timezone("America/New_York") do
            t.getlocal.zone.encoding.should == Encoding::US_ASCII
          end
        end
      end
    end

    it "doesn't raise errors for a Time with a fixed offset" do
      lambda {
        Time.new(2001, 1, 1, 0, 0, 0, "+05:00").zone
      }.should_not raise_error
    end
  end

  it "returns UTC when called on a UTC time" do
    Time.now.utc.zone.should == "UTC"
  end
end
