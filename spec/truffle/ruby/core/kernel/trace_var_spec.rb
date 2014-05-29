require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#trace_var" do
  before :each do
    $Kernel_trace_var_global = nil
  end
  
  after :each do
    untrace_var :$Kernel_trace_var_global
    $Kernel_trace_var_global = nil
  end
  
  it "is a private method" do
    Kernel.should have_private_instance_method(:trace_var)
  end
  
  it "hooks assignments to a global variable" do
    captured = nil
    
    trace_var :$Kernel_trace_var_global do |value|
      captured = value
    end
    
    $Kernel_trace_var_global = 'foo'
    captured.should == 'foo'
  end
  
  it "accepts a proc argument insted of a block" do
    captured = nil
    
    trace_var(
        :$Kernel_trace_var_global,
        proc {|value| captured = value})
        
    $Kernel_trace_var_global = 'foo'
    captured.should == 'foo'
  end
  
  it "raises ArgumentError if no block or proc is provided" do
    lambda do
      trace_var :$Kernel_trace_var_global
    end.should raise_error(ArgumentError)
  end
end

describe "Kernel.trace_var" do
  it "needs to be reviewed for spec completeness"
end
