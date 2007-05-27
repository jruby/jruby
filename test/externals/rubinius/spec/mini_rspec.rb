# mini_rspec.rb
#
# Very minimal set of features to support specs like this:
#
# context "Array" do
#   specify "should respond to new" do
#     Array.new.should == []
#   end
# end

class PositiveExpectation
  def initialize(obj)
    @obj = obj
  end
  
  def ==(other)
    unless @obj == other
      raise Exception.new("Equality expected for #{@obj.inspect} and #{other.inspect}")
    end
  end
end

class NegativeExpectation
  def initialize(obj)
    @obj = obj
  end
  
  def ==(other)
    if @obj == other
      raise Exception.new("Inequality expected for #{@obj.inspect} and #{other.inspect}")
    end
  end
end

class Object
  def should
    PositiveExpectation.new(self)
  end
  
  def should_not
    NegativeExpectation.new(self)
  end
end

@__before__ = []
@__after__ = []

def before(at=:each,&block)
  if at == :each
    @__before__.push block
  elsif at == :all
    STDOUT.print "mini_rspec does not support before(:all)"
  else
    raise ArgumentError, "I do not know when you want me to call your block"
  end
end

def after(at=:each,&block)
  if at == :each
    @__after__.push block
  elsif at == :all
    STDOUT.print "mini_rspec does not support after(:all)"
  else
    raise ArgumentError, "I do not know when you want me to call your block"
  end
end

def it(msg)
  STDOUT.print " - "
  STDOUT.print msg

  begin
    @__before__.each { |b| b.call }
    yield
    Mock.verify  

  rescue Exception => e
    STDOUT.print " FAILED:\n"

    if e.message != ""
      STDOUT.print e.message
      STDOUT.print ": "
      STDOUT.print "\n"
      STDOUT.print e.backtrace.show rescue STDOUT.print e.backtrace
    else
      STDOUT.print "<No message>"
    end

  # Cleanup
  ensure
    Mock.cleanup
    Mock.reset
    @__after__.each { |b| b.call }
  end

  STDOUT.print "\n"
end

def describe(msg)
  STDOUT.print msg
  STDOUT.print "\n-------------------\n"

  yield

  STDOUT.print "\n"
end

# Alternatives
class Object
  alias context describe
  alias specify it
  alias setup before
  alias teardown after
end
