require File.expand_path('../../spec_helper', __FILE__)

class SpecificExampleException < StandardError
end
class OtherCustomException < StandardError
end
class ArbitraryException < StandardError
end

exception_list = [SpecificExampleException, ArbitraryException]

describe "The rescue keyword" do
  before :each do
    ScratchPad.record []
  end

  it "can be used to handle a specific exception" do
    begin
      raise SpecificExampleException, "Raising this to be handled below"
    rescue SpecificExampleException
      :caught
    end.should == :caught
  end

  it "can capture the raised exception in a local variable" do
    begin
      raise SpecificExampleException, "some text"
    rescue SpecificExampleException => e
      e.message.should == "some text"
    end
  end

  it "can rescue multiple raised exceptions with a single rescue block" do
    [lambda{raise ArbitraryException}, lambda{raise SpecificExampleException}].map do |block|
      begin
        block.call
      rescue SpecificExampleException, ArbitraryException
        :caught
      end
    end.should == [:caught, :caught]
  end

  it "can rescue a splatted list of exceptions" do
    caught_it = false
    begin
      raise SpecificExampleException, "not important"
    rescue *exception_list
      caught_it = true
    end
    caught_it.should be_true
    caught = []
    [lambda{raise ArbitraryException}, lambda{raise SpecificExampleException}].each do |block|
      begin
        block.call
      rescue *exception_list
        caught << $!
      end
    end
    caught.size.should == 2
    exception_list.each do |exception_class|
      caught.map{|e| e.class}.should include(exception_class)
    end
  end

  it "will only rescue the specified exceptions when doing a splat rescue" do
    lambda do
      begin
        raise OtherCustomException, "not rescued!"
      rescue *exception_list
      end
    end.should raise_error(OtherCustomException)
  end

  it "will execute an else block only if no exceptions were raised" do
    result = begin
      ScratchPad << :one
    rescue
      ScratchPad << :does_not_run
    else
      ScratchPad << :two
      :val
    end
    result.should == :val
    ScratchPad.recorded.should == [:one, :two]
  end

  it "will not execute an else block if an exception was raised" do
    result = begin
      ScratchPad << :one
      raise "an error occurred"
    rescue
      ScratchPad << :two
      :val
    else
      ScratchPad << :does_not_run
    end
    result.should == :val
    ScratchPad.recorded.should == [:one, :two]
  end

  it "will not rescue errors raised in an else block in the rescue block above it" do
    lambda do
      begin
        ScratchPad << :one
      rescue Exception => e
        ScratchPad << :does_not_run
      else
        ScratchPad << :two
        raise SpecificExampleException, "an error from else"
      end
    end.should raise_error(SpecificExampleException)
    ScratchPad.recorded.should == [:one, :two]
  end

  it "parses  'a += b rescue c' as 'a += (b rescue c)'" do
    a = 'a'
    c = 'c'
    a += b rescue c
    a.should == 'ac'
  end

  it "without classes will not rescue Exception" do
    lambda do
      begin
        raise Exception
      rescue
        'Exception wrongly rescued'
      end
    end.should raise_error(Exception)
  end
end
