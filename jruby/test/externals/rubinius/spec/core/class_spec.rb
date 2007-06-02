# This file specs the methods available to Class. Syntax 
# related to Class (such as the class and def keywords) 
# is in spec/language/class_syntax_spec.rb
#
require File.dirname(__FILE__) + '/../spec_helper'

# Class methods:
#   .new              OK
#
# Instance methods:
#   #allocate         OK
#   #inherited        OK
#   #new              OK
#   #superclass       OK
#

context 'Using Class.new to create a new class' do
  specify 'Returns a new anonymous Class instance' do
    Class.new.class.should == Class
  end   

  specify 'May be given a Class argument to be used as superclass (Object by default)' do
    Class.new.superclass.should == Object
    Class.new(Array).superclass.should == Array
    Class.new(Enumerable) rescue :failure
  end

  specify 'If a block is provided, it is evaluated in the context of the Class object' do
    c = Class.new {NO = :inside; const_set :YES, :inside
                   @civ = :civ; @@cv = :cv; 
                   def self.foo; :class_foo; end 
                   define_method('foo') {:instance_foo}}

    c.constants.should == ['YES']
    c.instance_variables.should == ['@civ']
    c.class_variables.should == ['@@cv']
    c.foo.should == :class_foo
    c.new.foo.should == :instance_foo
  end

  specify "The generated class can be instantiated" do
    HasNoParents = Class.new
    HasNoParents.new.class.should == HasNoParents
  end
  
  specify "The generated class inherits its parent's methods" do
    class Biped 
      def self.legs; 2 end 
      def has_legs?; true end
    end

    klass = Class.new(Biped)
    klass.superclass.should == Biped

    klass.legs.should == 2
    klass.new.has_legs?.should == true
  end
end


context 'Class object instantiation' do
  setup do
    class ClassSpec
      def initialize(); @a = true; end
      def foobar(); :foobar; end
    end
  end

  specify '#allocate allocates space for the object but does not run the instance method #initialize' do
    cs = ClassSpec.allocate
    cs.instance_variables.should == []
    cs.foobar.should == :foobar
  end

  specify '#new allocates space for the object and runs the instance method #initialize' do
    cs = ClassSpec.new
    cs.instance_variables.should == ['@a']
    cs.foobar.should == :foobar
  end
end


context 'Class event hook methods' do
  specify '#inherited, if implemented, is called when a Class object is inherited. Subclass object given as parameter.' do
    class ClassSpecNotInherited
      @@i = nil
    end
    
    class ClassSpecInherited
      @@i = nil
      def self.inherited(by); @@i = by; end
    end
     
    Class.new ClassSpecNotInherited
    i = Class.new ClassSpecInherited

    ClassSpecNotInherited.send('class_variable_get', '@@i').should == nil
    ClassSpecInherited.send('class_variable_get', '@@i').should == i
  end
end

context 'Instantiated Class object' do
  specify 'Makes its superclass object available through #superclass' do
    class CS_SuperA; end
    class CS_SuperB < Array; end
    CS_SuperC = Class.new
    CS_SuperD = Class.new Array
    CS_SuperE = Class.new CS_SuperC

    CS_SuperA.superclass.should == Object 
    CS_SuperB.superclass.should == Array 
    CS_SuperC.superclass.should == Object 
    CS_SuperD.superclass.should == Array
    CS_SuperE.superclass.superclass.should == Object
  end
end


context 'Accessing class variables of a Class' do
  specify 'class variables may be read using .class_variable_get which takes a Symbol or a String' do
    class CS_CV2; @@a = 1; end
    
    CS_CV2.send(:class_variable_get, :@@a).should == 1
    CS_CV2.send(:class_variable_get, '@@a').should == 1
  end

  specify 'the name given to class_variable_get must begin with @@' do
    class CS_CV3; @@a = 1; end

    should_raise(NameError) { CS_CV3.send(:class_variable_get, :a) }
    should_raise(NameError) { CS_CV3.send(:class_variable_get, 'a') }
  end

  specify 'class variables may be set using .class_variable_set which takes a Symbol or a String' do
    class CS_CV4; @@a = 1; end
    
    CS_CV4.send(:class_variable_set, :@@b, 2)
    CS_CV4.send(:class_variable_get, :@@b).should == 2
    CS_CV4.send(:class_variable_get, :@@a).should == 1

    CS_CV4.send(:class_variable_set, :@@c, 3)
    CS_CV4.send(:class_variable_get, '@@c').should == 3
    CS_CV4.send(:class_variable_get, '@@a').should == 1
  end
  
  specify 'class_variable_set will overwrite the previous value' do
    class CS_CV5; @@a = 1; end
    
    CS_CV5.send(:class_variable_set, :@@a, 2)
    CS_CV5.send(:class_variable_get, :@@a).should == 2
    CS_CV5.send(:class_variable_get, '@@a').should == 2
  end

  specify 'class_variable_set returns the new value' do
    class CS_CV6; @@a = 1; end
    
    CS_CV6.send(:class_variable_set, :@@b, 2).should == 2
  end

  specify 'the name given to class_variable_set must begin with @@' do
    class CS_CV7; ; end

    should_raise(NameError) { CS_CV7.send(:class_variable_set, :a, 1) }
    should_raise(NameError) { CS_CV7.send(:class_variable_set, 'a', 1) }
  end

  specify 'class variables set using class_variable_set are available as @@ vars' do
    class CS_CV8; @@a = nil; end

    CS_CV8.send(:class_variable_set, :@@a, 1)

    class CS_CV8; @@a.should == 1; end
  end
end
