require File.dirname(__FILE__) + "/../spec_helper"

require 'java'

describe "The $CLASSPATH variable" do
  it "appends URLs unmodified" do
    $CLASSPATH << "http://jruby.org/"
    $CLASSPATH.should include("http://jruby.org/")
  end

  it "assumes entries without URL protocols are files" do
    $CLASSPATH << __FILE__
    $CLASSPATH.should include("file:#{__FILE__}")
  end

  it "appends slashes to directory names" do
    d = File.expand_path(File.dirname(__FILE__))
    $CLASSPATH << d
    $CLASSPATH.should include("file:#{d}/")
  end
end
