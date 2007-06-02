require File.dirname(__FILE__) + '/../spec_helper'

# ===, id2name, inspect, to_i, to_int, to_s, to_sym

context "Symbol instance method" do
  specify "=== should return true if other is the same symbol" do
    sym = :ruby
    (sym === :ruby).should == true
    (:foo === :bar).should == false
    (:one === 'one'.intern).should == true
    (:nope === 'nope').should == false
    (:yep === 'yep'.to_sym).should == true
  end
  
  specify "id2name should return the string corresponding to self" do
    :rubinius.id2name.should == "rubinius"
    :squash.id2name.should == "squash"
    'string'.to_sym.id2name.should == "string"
  end
  
  specify "inspect should return the representation of self as a symbol literal" do
    :ruby.inspect.should == ":ruby"
    :file.inspect.should == ":file"
  end
  
  specify "to_i should return an integer for a symbol" do
    :ruby.to_i.kind_of?(Integer).should == true
    'rubinius'.to_sym.to_i.kind_of?(Integer).should == true
  end
  
  specify "to_int should be a synonym for to_i" do
    :ruby.to_i.kind_of?(Integer).should == true
    'rubinius'.to_sym.to_i.kind_of?(Integer).should == true
  end
  
  specify "to_s should be a synonym for id2name" do
    :rubinius.to_s.should == "rubinius"
    :squash.to_s.should == "squash"
    'string'.to_sym.to_s.should == "string"
  end
  
  specify "to_sym should return self" do
    :rubinius.to_sym.should == :rubinius
    :ruby.to_sym.should == :ruby
  end  
end
