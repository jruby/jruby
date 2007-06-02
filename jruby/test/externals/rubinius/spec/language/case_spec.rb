require File.dirname(__FILE__) + '/../spec_helper'

describe "The 'case'-construct" do
  it "should evaluate the body of the when clause matching the case target expression" do
    case 1
      when 2: false
      when 1: true
    end.should == true
  end

  it "should evaluate the body of the when clause whose array expression includes the case target expression" do
    case 2
      when 3, 4: false
      when 1, 2: true
    end.should == true
  end

  it "should evaluate the body of the when clause whose range expression includes the case target expression" do
    case 5
      when 21..30: false
      when 1..20: true
    end.should == true
  end

  it "should return nil when no 'then'-bodies are given" do
    case "a"
      when "a"
      when "b"
    end.should == nil
  end
  
  it "should evaluate the 'else'-body when no expression matched" do
    case "c"
      when "a": 'foo'
      when "b": 'bar'
      else 'zzz'
    end.should == 'zzz'
  end
  
  it "should return nil when no expression matched and 'else'-body is empty" do
    case "c"
      when "a": "a"
      when "b": "b"
      else
    end.should == nil
  end
  
  it "should evaluate the body of the first when clause that is not false/nil when no case target expression is given" do
    case
      when false: 'foo'
      when nil: 'foo'
      when 1 == 1: 'bar'
    end.should == 'bar'
  end
    
  it "should return the statement following ':'" do
    case "a"
      when "a": 'foo'
      when "b": 'bar'
    end.should == 'foo'
  end
    
  it "should return the statement following 'then'" do
    case "a"
      when "a" then 'foo'
      when "b" then 'bar'
    end.should == 'foo'
  end
    
  it "should allow mixing ':' and 'then'" do
    case "b"
      when "a": 'foo'
      when "b" then 'bar'
    end.should == 'bar'
  end
    
  it "should test with class equality" do
    case "a"
      when String
        'foo'
      when Symbol
        'bar'
    end.should == 'foo'
  end
  
  it "should test with matching regexps" do
    case "hello"
      when /abc/: false
      when /^hell/: true
    end.should == true
  end
  
  it "should not test with equality when given classes" do
    case :symbol.class
      when Symbol
        "bar"
      when String
        "bar"
      else
        "foo"
    end.should == "foo"
  end
  
  it "should take lists of values" do
    case 'z'
      when 'a', 'b', 'c', 'd'
        "foo" 
      when 'x', 'y', 'z'
        "bar" 
    end.should == "bar"
  end
  
  it "should expand arrays to lists of values" do
    case 'z'
      when *['a', 'b', 'c', 'd']
        "foo" 
      when *['x', 'y', 'z']
        "bar" 
    end.should == "bar"
  end

  it "should take an expanded array in addition to a list of values" do
    case 'f'
      when 'f', *['a', 'b', 'c', 'd']
        "foo" 
      when *['x', 'y', 'z']
        "bar" 
    end.should == "foo"
  end

  it "should concat arrays before expanding them" do
    a = ['a', 'b', 'c', 'd']
    b = ['f']
  
    case 'f'
      when 'f', *a|b
        "foo" 
      when *['x', 'y', 'z']
        "bar" 
    end.should == "foo"
  end
  
  it "should let you define a method after the case statement" do
    case (def foo; 'foo'; end; 'f')
      when 'a'
        'foo'
      when 'f'
        'bar'
    end.should == 'bar'
  end
  
  it "should raise a SyntaxError when 'else' is used when no 'when' is given" do
    should_raise(SyntaxError) do
      eval <<-CODE
      case 4
        else
          true
      end
      CODE
    end
  end

  it "should raise a SyntaxError when 'else' is used before a 'when' was given" do
    should_raise(SyntaxError) do
      eval <<-CODE
      case 4
        else
          true
        when 4: false
      end
      CODE
    end
  end
end
