require File.dirname(__FILE__) + '/../spec_helper'

# Binding has no direct creation
context 'Creating Bindings' do
  specify 'No .new provided' do
    should_raise(NoMethodError) {Binding.new}
  end  

  specify 'Kernel.binding creates a new Binding' do
    Kernel.binding.class.should == Binding  
    binding.class.should == Binding 
  end
end

# Instance methods: #clone, #dup
context 'Initialised Binding' do
  setup do
    @o = Object.new
    def @o.get_binding()
      value = 1
      binding
    end
  end

  specify 'May be duplicated with #dup' do
    b = @o.get_binding
    eval('value', b).should == eval('value', b.dup)
  end  

  specify 'May be cloned with #clone' do
    b = @o.get_binding
    eval('value', b).should == eval('value', b.clone)
  end  

  
  specify 'Normal #dup and #clone semantics apply' do
    d, c = @o.get_binding, @o.get_binding

    def d.single?(); true; end
    def c.single?(); true; end

    d.freeze
    c.freeze

    d.dup.frozen?.should == false 
    d.dup.methods.include?('single?').should == false
    c.clone.frozen?.should == true 
    c.clone.methods.include?('single?').should == true

    eval('value', d).should == eval('value', d.dup) 
    eval('value', c).should == eval('value', c.clone) 
  end  
end
