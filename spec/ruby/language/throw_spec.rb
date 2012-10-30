require File.expand_path('../../spec_helper', __FILE__)

describe "The throw keyword" do
  it "abandons processing" do
    i = 0
    catch(:done) do
      loop do
        i += 1
        throw :done if i > 4
      end
      i += 1
    end
    i.should == 5
  end

  it "supports a second parameter" do
    msg = catch(:exit) do
      throw :exit,:msg
    end
    msg.should == :msg
  end

  it "uses nil as a default second parameter" do
    msg = catch(:exit) do
      throw :exit
    end
    msg.should == nil
  end

  ruby_version_is "" ... "1.9" do
    it "only allows symbols and strings as its argument" do
      lambda { throw 1 }.should raise_error(ArgumentError)
      lambda { throw Object.new }.should raise_error(TypeError)
    end
  end

  ruby_version_is "1.9" do
    it "only any object as its argument" do
      lambda { catch(1) { throw 1 } }.should_not raise_error
      lambda { o = Object.new; catch(o) { throw o } }.should_not raise_error
    end
  end

  ruby_version_is "" ... "1.9" do
    it "converts strings to a symbol" do
      lambda { catch(:exit) { throw "exit" } }.should_not raise_error
      a = "exit"
      lambda { catch(:exit) { throw a } }.should_not raise_error
    end
  end

  ruby_version_is "1.9" do
    it "does not convert strings to a symbol" do
      lambda { catch(:exit) { throw "exit" } }.should raise_error(ArgumentError)
    end
  end

  it "unwinds stack from within a method" do
    def throw_method(handler,val)
      throw handler,val
    end

    catch(:exit) do
      throw_method(:exit,5)
    end.should == 5
  end

  it "unwinds stack from within a lambda" do
    c = lambda { throw :foo, :msg }
    catch(:foo) { c.call }.should == :msg
  end

  ruby_version_is "" ... "1.9" do
    it "raises a NameError if outside of scope of a matching catch" do
      lambda { throw :test,5 }.should raise_error(NameError)
      lambda { catch(:different) { throw :test,5 } }.should raise_error(NameError)
    end
  end

  ruby_version_is "1.9" do
    it "raises an ArgumentError if outside of scope of a matching catch" do
      lambda { throw :test,5 }.should raise_error(ArgumentError)
      lambda { catch(:different) { throw :test,5 } }.should raise_error(ArgumentError)
    end
  end

  ruby_version_is "" ... "1.9" do
    it "raises a ThreadError if used to exit a thread" do
      lambda {
        catch(:what) do
          Thread.new do
            throw :what
          end.join
        end
      }.should raise_error(ThreadError)
    end
  end

  ruby_version_is "1.9" do
    it "raises a ArgumentError if used to exit a thread" do
      lambda {
        catch(:what) do
          Thread.new do
            throw :what
          end.join
        end
      }.should raise_error(ArgumentError)
    end
  end
end
