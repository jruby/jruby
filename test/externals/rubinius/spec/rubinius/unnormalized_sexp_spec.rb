require File.dirname(__FILE__) + '/../spec_helper'
require File.dirname(__FILE__) + '/../sexp_expectations'

class String
  alias :old_to_sexp :to_sexp
  def to_sexp
    old_to_sexp('test', 1, false)
  end
end

context "Producing sexps from source code" do
  SEXP_EXPECTATIONS.each do |node, hash|
    specify "should succeed for a node of type :#{node}" do
      hash['Ruby'].to_sexp.should == hash['ParseTree']
    end
  end
end
