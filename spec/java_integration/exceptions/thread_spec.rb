require File.dirname(__FILE__) + "/../spec_helper"

describe "A Java exception bubbling out of a Ruby Thread" do
  it "propagates out of Thread#join" do
    t = Thread.new { raise java.lang.NullPointerException.new }
    begin
      t.join
      fail "should not reach here"
    rescue => e
      expect(e.class).to equal(java.lang.NullPointerException)
    end
  end

  it "propagates out of Thread#value" do
    t = Thread.new { raise java.lang.NullPointerException.new }
    begin
      Thread.pass while t.status
      t.value
      fail "should not reach here"
    rescue => e
      expect(e.class).to equal(java.lang.NullPointerException)
    end
  end
end