require File.dirname(__FILE__) + '/../spec_helper'

# class methods
#  exception, new

# instance methods
#  backtrace, exception, inspect, message, set_backtrace, to_s,
#  to_str, to_yaml

def check_ancestor(cls1, cls2)
  cls1.ancestors.include?(cls2).should == true
end

context "Exception class method" do
  specify "new should create a new instance of Exception" do
    check_ancestor(Exception.new.class, Exception)
  end
  
  specify "new with message should set the message of the Exception" do
    Exception.new("I'm broken.").message.should == "I'm broken."
  end
  
  specify "exception should be a synonym for new" do
    check_ancestor(Exception.exception.class, Exception)
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
#    defined?(Exception).should == "constant"
    Exception
  end
  
  specify "NoMemoryError should subclass Exception" do
    check_ancestor(NoMemoryError, Exception)
  end

  specify "ScriptError should subclass Exception" do
    check_ancestor(ScriptError, Exception)
  end
  
  specify "LoadError should subclass ScriptError" do
    check_ancestor(LoadError, ScriptError)
  end

  specify "NotImplementedError should subclass ScriptError" do
    check_ancestor(NotImplementedError, ScriptError)
  end

  specify "SyntaxError should subclass ScriptError" do
    check_ancestor(SyntaxError, ScriptError)
  end

  specify "SignalException should subclass Exception" do
    check_ancestor(SignalException, Exception)
  end
  
  specify "Interrupt should subclass SignalException" do
    check_ancestor(Interrupt, SignalException)
  end

  specify "StandardError should subclass Exception" do
    check_ancestor(StandardError, Exception)
  end
  
  specify "ArgumentError should subclass StandardError" do
    check_ancestor(ArgumentError, StandardError)
  end

  specify "IOError should subclass StandardError" do
    check_ancestor(IOError, StandardError)
  end

  specify "EOFError should subclass IOError" do
    check_ancestor(EOFError, IOError)
  end
  
  specify "IndexError should subclass StandardError" do
    check_ancestor(IndexError, StandardError)
  end

  specify "LocalJumpError should subclass StandardError" do
    check_ancestor(LocalJumpError, StandardError)
  end

  specify "NameError should subclass StandardError" do
    check_ancestor(NameError, StandardError)
  end
  
  specify "NameError.new should take optional name argument" do
    NameError.new("msg","name").name.should == "name"
  end
  
  specify "NoMethodError should subclass NameError" do
    check_ancestor(NoMethodError, NameError)
  end
  
  specify "NoMethodError.new should allow passing method args" do
    NoMethodError.new("msg","name","args").args.should == "args"
  end

  specify "RangeError should subclass StandardError" do
    check_ancestor(RangeError, StandardError)
  end
  
  specify "FloatDomainError should subclass RangeError" do
    check_ancestor(FloatDomainError, RangeError)
  end

  specify "RegexpError should subclass StandardError" do
    check_ancestor(RegexpError, StandardError)
  end

  specify "RuntimeError should subclass StandardError" do
    check_ancestor(RuntimeError, StandardError)
  end

  specify "SecurityError should subclass StandardError" do
    check_ancestor(SecurityError, StandardError)
  end

  specify "SystemCallError should subclass StandardError" do
    check_ancestor(SystemCallError.new("").class, StandardError)
  end
  
  specify "SystemCallError.new requires at least one argumentt" do
    should_raise(ArgumentError) { SystemCallError.new }
  end
  
  specify "SystemCallError.new should take optional errno argument" do
    check_ancestor(SystemCallError.new("message",1).class, SystemCallError)
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
    check_ancestor(ThreadError, StandardError)
  end

  specify "TypeError should subclass StandardError" do
    check_ancestor(TypeError, StandardError)
  end

  specify "ZeroDivisionError should subclass StandardError" do
    check_ancestor(ZeroDivisionError, StandardError)
  end

  specify "SystemExit should subclass Exception" do
    check_ancestor(SystemExit, Exception)
  end

  specify "SystemStackError should subclass Exception" do
    check_ancestor(SystemStackError, Exception)
  end
end

