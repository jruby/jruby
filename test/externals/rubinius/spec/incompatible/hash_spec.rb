require File.dirname(__FILE__) + '/../spec_helper'

# Class methods:
#   .[]

# Parser chockes on this as of 620
context 'Creating a Hash' do
 specify 'Hash[] is able to process key, val arguments followed by key => val arguments' do
   Hash[:a, 1, :b, 2, :c => 3].should == {:a => 1, :b => 2, :c => 3}
 end
end
