require File.dirname(__FILE__) + '/../spec_helper'


context 'Accessing class variables through Objects' do
  class OIS_CV1 
    def class_variable_set(*); :cvset; end
    def class_variable_get(*); :cvget; end 
  end

  specify 'using #class_variable_set is equivalent to self.class.class_variable_set' do
    OIS_CV1.new.class_variable_set(:@@a, 1).should == :cvset 
  end

  specify 'using #class_variable_get is equivalent to self.class.class_variable_get' do
    OIS_CV1.new.class_variable_get(:@@a).should == :cvget 
  end
end
