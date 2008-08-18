require File.dirname(__FILE__) + "/../spec_helper"

describe "Java invocation errors" do
  describe "(calling constructor)" do 
    it "should fail correctly when called with wrong parameters" do 
      proc do 
        java.util.HashMap.new "str"
      end.should raise_error(NameError)
    end
  end
  
  describe "(calling instance method)" do 
    it "should fail correctly when called with wrong parameters" do 
      proc do 
        java.util.ArrayList.new.add_all "str"
      end.should raise_error(NameError)
    end
  end
end
