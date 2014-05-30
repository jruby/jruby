require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../fixtures/kernel/classes', __FILE__)

describe :method_missing, :shared => true do
  it "is a private method" do
    @object.should have_private_instance_method(:method_missing)
  end
end

describe :method_missing_defined_module, :shared => true do
  describe "for a Module with #method_missing defined" do
    it "is not called when a defined method is called" do
      @object.method_public.should == :module_public_method
    end

    it "is called when an undefined method is called" do
      @object.method_undefined.should == :module_method_missing
    end

    it "is called when an protected method is called" do
      @object.method_protected.should == :module_method_missing
    end

    it "is called when an private method is called" do
      @object.method_private.should == :module_method_missing
    end
  end
end

describe :method_missing_module, :shared => true do
  describe "for a Module" do
    it "raises a NoMethodError when an undefined method is called" do
      lambda { @object.method_undefined }.should raise_error(NoMethodError)
    end

    it "raises a NoMethodError when a protected method is called" do
      lambda { @object.method_protected }.should raise_error(NoMethodError)
    end

    it "raises a NoMethodError when a private method is called" do
      lambda { @object.method_private }.should raise_error(NoMethodError)
    end
  end
end

describe :method_missing_defined_class, :shared => true do
  describe "for a Class with #method_missing defined" do
    it "is not called when a defined method is called" do
      @object.method_public.should == :class_public_method
    end

    it "is called when an undefined method is called" do
      @object.method_undefined.should == :class_method_missing
    end

    it "is called when an protected method is called" do
      @object.method_protected.should == :class_method_missing
    end

    it "is called when an private method is called" do
      @object.method_private.should == :class_method_missing
    end
  end
end

describe :method_missing_class, :shared => true do
  describe "for a Class" do
    it "raises a NoMethodError when an undefined method is called" do
      lambda { @object.method_undefined }.should raise_error(NoMethodError)
    end

    it "raises a NoMethodError when a protected method is called" do
      lambda { @object.method_protected }.should raise_error(NoMethodError)
    end

    it "raises a NoMethodError when a private method is called" do
      lambda { @object.method_private }.should raise_error(NoMethodError)
    end
  end
end

describe :method_missing_defined_instance, :shared => true do
  describe "for an instance with #method_missing defined" do
    before :each do
      @instance = @object.new
    end

    it "is not called when a defined method is called" do
      @instance.method_public.should == :instance_public_method
    end

    it "is called when an undefined method is called" do
      @instance.method_undefined.should == :instance_method_missing
    end

    it "is called when an protected method is called" do
      @instance.method_protected.should == :instance_method_missing
    end

    it "is called when an private method is called" do
      @instance.method_private.should == :instance_method_missing
    end
  end
end

describe :method_missing_instance, :shared => true do
  describe "for an instance" do
    it "raises a NoMethodError when an undefined method is called" do
      lambda { @object.new.method_undefined }.should raise_error(NoMethodError)
    end

    it "raises a NoMethodError when a protected method is called" do
      lambda { @object.new.method_protected }.should raise_error(NoMethodError)
    end

    it "raises a NoMethodError when a private method is called" do
      lambda { @object.new.method_private }.should raise_error(NoMethodError)
    end
  end
end
