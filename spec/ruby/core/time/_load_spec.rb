require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/methods', __FILE__)

describe "Time#_load" do
  ruby_bug("http://redmine.ruby-lang.org/issues/show/627", "1.8.7") do
    it "loads a time object in the new format" do
      t = Time.local(2000, 1, 15, 20, 1, 1)
      t = t.gmtime

      high =               1 << 31 |
            (t.gmt? ? 1 : 0) << 30 |
             (t.year - 1900) << 14 |
                (t.mon  - 1) << 10 |
                       t.mday << 5 |
                            t.hour

      low =  t.min  << 26 |
             t.sec  << 20 |
                   t.usec

      Time._load([high, low].pack("VV")).should == t
    end
  end

  it "loads a time object in the old UNIX timestamp based format" do
    t = Time.local(2000, 1, 15, 20, 1, 1, 203)
    timestamp = t.to_i

    high = timestamp & ((1 << 31) - 1)

    low =  t.usec

    Time._load([high, low].pack("VV")).should == t
  end

  ruby_version_is ''...'1.9' do
    it "loads MRI's marshaled time format" do
      t = Marshal.load("\004\bu:\tTime\r\320\246\e\200\320\001\r\347")
      t.utc

      t.to_s.should == "Fri Oct 22 16:57:48 UTC 2010"
    end
  end

  ruby_version_is '1.9' do
    it "loads MRI's marshaled time format" do
      t = Marshal.load("\004\bu:\tTime\r\320\246\e\200\320\001\r\347")
      t.utc

      t.to_s.should == "2010-10-22 16:57:48 UTC"
    end
  end
end

describe "Time._load" do
  it "needs to be reviewed for spec completeness"
end
