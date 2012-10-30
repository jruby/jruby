require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel.warn" do
  it "is a private method" do
    Kernel.should have_private_instance_method(:warn)
  end

  it "calls #write on $stderr if $VERBOSE is true" do
    lambda {
      v = $VERBOSE
      $VERBOSE = true

      warn("this is some simple text")

      $VERBOSE = v
    }.should output(nil, "this is some simple text\n")
  end

  it "calls #write on $stderr if $VERBOSE is false" do
    lambda {
      v = $VERBOSE
      $VERBOSE = false

      warn("this is some simple text")

      $VERBOSE = v
    }.should output(nil, "this is some simple text\n")
  end

  it "does not call #write on $stderr if $VERBOSE is nil" do
    lambda {
      v = $VERBOSE
      $VERBOSE = nil

      warn("this is some simple text")

      $VERBOSE = v
    }.should output(nil, "")
  end

  it "writes the default record separator and NOT $/ to $stderr after the warning message" do
    lambda {
      v = $VERBOSE
      rs = $/
      $VERBOSE = true
      $/ = 'rs'

      warn("")

      $VERBOSE = v
      $/ = rs
    }.should output(nil, /\n/)
  end
end

describe "Kernel#warn" do
  it "needs to be reviewed for spec completeness"
end
