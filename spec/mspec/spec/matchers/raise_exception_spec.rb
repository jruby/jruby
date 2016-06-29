require 'spec_helper'
require 'mspec/expectations/expectations'
require 'mspec/matchers'

class ExpectedException < Exception; end
class UnexpectedException < Exception; end

describe RaiseExceptionMatcher do
  it "is a legac alias of RaiseErrorMatcher" do
    RaiseExceptionMatcher.should equal(RaiseErrorMatcher)
  end
end
