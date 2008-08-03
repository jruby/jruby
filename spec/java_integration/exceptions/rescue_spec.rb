require File.dirname(__FILE__) + "/../spec_helper"

import java.lang.NullPointerException

describe "Non-wrapped Java exceptions" do
  it "can be rescued" do
    exception = NullPointerException.new
    begin
      raise exception
    rescue NullPointerException => npe
    end
    
    npe.should_not == nil
    npe.should == exception
  end
end