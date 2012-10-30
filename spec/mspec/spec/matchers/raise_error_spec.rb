require 'spec_helper'
require 'mspec/expectations/expectations'
require 'mspec/matchers'

class ExpectedException < Exception; end
class UnexpectedException < Exception; end

describe RaiseErrorMatcher do
  it "matches when the proc raises the expected exception" do
    proc = Proc.new { raise ExpectedException }
    RaiseErrorMatcher.new(ExpectedException, nil).matches?(proc).should == true
  end

  it "executes it's optional block if matched" do
    run = false
    proc = Proc.new { raise ExpectedException }
    matcher = RaiseErrorMatcher.new(ExpectedException, nil) { |error|
      run = true
      error.class.should == ExpectedException
    }

    matcher.matches?(proc).should == true
    run.should == true
  end

  it "matches when the proc raises the expected exception with the expected message" do
    proc = Proc.new { raise ExpectedException, "message" }
    RaiseErrorMatcher.new(ExpectedException, "message").matches?(proc).should == true
  end

  it "does not match when the proc does not raise the expected exception" do
    proc = Proc.new { raise UnexpectedException }
    RaiseErrorMatcher.new(ExpectedException, nil).matches?(proc).should == false
  end

  it "does not match when the proc raises the expected exception with an unexpected message" do
    proc = Proc.new { raise ExpectedException, "unexpected" }
    RaiseErrorMatcher.new(ExpectedException, "expected").matches?(proc).should == false
  end

  it "does not match when the proc does not raise an exception" do
    proc = Proc.new {}
    RaiseErrorMatcher.new(ExpectedException, "expected").matches?(proc).should == false
  end

  it "provides a useful failure message" do
    proc = Proc.new { raise UnexpectedException, "unexpected" }
    matcher = RaiseErrorMatcher.new(ExpectedException, "expected")
    matcher.matches?(proc)
    matcher.failure_message.should ==
      ["Expected ExpectedException (expected)", "but got UnexpectedException (unexpected)"]
  end

  it "provides a useful negative failure message" do
    proc = Proc.new { raise ExpectedException, "expected" }
    matcher = RaiseErrorMatcher.new(ExpectedException, "expected")
    matcher.matches?(proc)
    matcher.negative_failure_message.should ==
      ["Expected to not get ExpectedException (expected)", ""]
  end

  it "provides a useful negative failure message for strict subclasses of the matched exception class" do
    proc = Proc.new { raise UnexpectedException, "unexpected" }
    matcher = RaiseErrorMatcher.new(Exception, nil)
    matcher.matches?(proc)
    matcher.negative_failure_message.should ==
      ["Expected to not get Exception", "but got UnexpectedException (unexpected)"]
  end
end
