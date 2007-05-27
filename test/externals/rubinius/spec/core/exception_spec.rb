require File.dirname(__FILE__) + '/../spec_helper'

# class methods
#  exception, new

# instance methods
#  backtrace, exception, inspect, message, set_backtrace, to_s,
#  to_str, to_yaml

context "Exception class method" do
  specify "new should create a new instance of Exception" do
    Exception.new.class.ancestors.should_include Exception
  end
  
  specify "new with message should set the message of the Exception" do
    Exception.new("I'm broken.").message.should == "I'm broken."
  end
  
  specify "exception should be a synonym for new" do
    Exception.exception.class.ancestors.should_include Exception
  end
  
  specify "exception should return 'Exception' for message when no message given" do
    Exception.exception.message.should == "Exception"
  end
  
  specify "exception with message should set the message of the Exception" do
    Exception.exception("Help, I've fallen.").message.should == "Help, I've fallen."
  end
end

context "Exception instance method" do
  specify "message should return the exception message" do
    [Exception.new.message, Exception.new("Ouch!").message].should == ["Exception", "Ouch!"]
  end
  
  specify "inspect should return '#<Exception: Exception>' when no message given" do
    Exception.new.inspect.should == "#<Exception: Exception>"
  end
  
  specify "inspect should include message when given" do
    [Exception.new("foobar").inspect].should == ["#<Exception: foobar>"]
  end
end

context "In the Exception class hierarchy" do
  specify "Exception should exist" do
    Exception.new.class.ancestors.should_include Exception
  end
  
  specify "NoMemoryError should subclass Exception" do
    NoMemoryError.new.class.ancestors.should_include Exception
  end

  specify "ScriptError should subclass Exception" do
    ScriptError.new.class.ancestors.should_include Exception
  end
  
  specify "LoadError should subclass ScriptError" do
    LoadError.new.class.ancestors.should_include ScriptError
  end

  specify "NotImplementedError should subclass ScriptError" do
    NotImplementedError.new.class.ancestors.should_include ScriptError
  end

  specify "SyntaxError should subclass ScriptError" do
    SyntaxError.new.class.ancestors.should_include ScriptError
  end

  specify "SignalException should subclass Exception" do
    SignalException.new.class.ancestors.should_include Exception
  end
  
  specify "Interrupt should subclass SignalException" do
    Interrupt.new.class.ancestors.should_include SignalException
  end

  specify "StandardError should subclass Exception" do
    StandardError.new.class.ancestors.should_include Exception
  end
  
  specify "ArgumentError should subclass StandardError" do
    ArgumentError.new.class.ancestors.should_include StandardError
  end

  specify "IOError should subclass StandardError" do
    IOError.new.class.ancestors.should_include StandardError
  end

  specify "EOFError should subclass IOError" do
    EOFError.new.class.ancestors.should_include IOError
  end
  
  specify "IndexError should subclass StandardError" do
    IndexError.new.class.ancestors.should_include StandardError
  end

  specify "LocalJumpError should subclass StandardError" do
    LocalJumpError.new.class.ancestors.should_include StandardError
  end

  specify "NameError should subclass StandardError" do
    NameError.new.class.ancestors.should_include StandardError
  end
  
  specify "NameError.new should take optional name argument" do
    NameError.new("msg","name").name.should == "name"
  end
  
  specify "NoMethodError should subclass NameError" do
    NoMethodError.new.class.ancestors.should_include NameError
  end
  
  specify "NoMethodError.new should allow passing method args" do
    NoMethodError.new("msg","name","args").args.should == "args"
  end

  specify "RangeError should subclass StandardError" do
    RangeError.new.class.ancestors.should_include StandardError
  end
  
  specify "FloatDomainError should subclass RangeError" do
    FloatDomainError.new.class.ancestors.should_include RangeError
  end

  specify "RegexpError should subclass StandardError" do
    RegexpError.new.class.ancestors.should_include StandardError
  end

  specify "RuntimeError should subclass StandardError" do
    RuntimeError.new.class.ancestors.should_include StandardError
  end

  specify "SecurityError should subclass StandardError" do
    SecurityError.new.class.ancestors.should_include StandardError
  end

  specify "SystemCallError should subclass StandardError" do
    SystemCallError.new("").class.ancestors.should_include StandardError
  end
  
  specify "SystemCallError.new requires at least one argumentt" do
    should_raise(ArgumentError) { SystemCallError.new }
  end
  
  specify "SystemCallError.new should take optional errno argument" do
    SystemCallError.new("message",1).class.ancestors.should_include SystemCallError
  end
  
  context "SystemCallError instance methods" do
    specify "errno should return nil when no errno given" do
      SystemCallError.new("message").errno.should == nil
    end  
    
    specify "errno should return the errno given as optional argument to new" do
      SystemCallError.new("message", 42).errno.should == 42
    end
  end

  specify "ThreadError should subclass StandardError" do
    ThreadError.new.class.ancestors.should_include StandardError
  end

  specify "TypeError should subclass StandardError" do
    TypeError.new.class.ancestors.should_include StandardError
  end

  specify "ZeroDivisionError should subclass StandardError" do
    ZeroDivisionError.new.class.ancestors.should_include StandardError
  end

  specify "SystemExit should subclass Exception" do
    SystemExit.new.class.ancestors.should_include Exception
  end

  specify "SystemStackError should subclass Exception" do
    SystemStackError.new.class.ancestors.should_include Exception
  end
end
