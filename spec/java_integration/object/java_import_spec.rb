require 'java'

describe "An object wanting to import Java classes" do
  it "should explode when trying to import a non-existant Java class" do
    lambda{ java_import 'does.not.Exist' }.should raise_error(NameError)
  end

  it "should receive an array with the imported classes" do
    java_import().should == []
    java_import('java.util.Hashtable').should == [java.util.Hashtable]

    imported_classes = java_import('java.util.Hashtable', java.util.Hashtable, Java::java.util.Hashtable, Java::JavaUtil::Hashtable)
    imported_classes.should == [java.util.Hashtable] * 4
  end
end

