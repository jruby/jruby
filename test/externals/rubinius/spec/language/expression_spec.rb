require File.dirname(__FILE__) + '/../spec_helper'

describe "The if expression" do
  it "evaluates body when the if expression is true" do
    if true: true; end.should == true
    if true then true; end.should == true
    if true
      true
    end.should == true
  end
  
  it "does not evaluate body when the if expression is false" do
    if false: true; end.should == nil
    if false then true; end.should == nil
    if false
      true
    end.should == nil
  end
  
  it "does not evaluate the else body when the if expression is true" do
    if true: true; else false; end.should == true
    if true then true; else false; end.should == true
    if true: true
    else
      false
    end.should == true
    if true
      true
    else
      false
    end.should == true
  end
  
  it "evaluates the else body when the if expression is false" do
    if false: true; else false; end.should == false
    if false then true; else false; end.should == false
    if false: true
    else
      false
    end.should == false
    if false
      true
    else
      false
    end.should == false
  end
end

describe "The unless expression" do
  it "evaluates the else body when the unless expression is false" do
    unless false: true; else false; end.should == true
    unless false then true; else false; end.should == true
    unless false: true
    else 
      false
    end.should == true
    unless false
      true
    else
      false
    end.should == true
  end
end

describe "The case expression" do
  it "evaluates the body of the when clause whose expression matches the case target expression" do
    case 1
    when 1: true
    end.should == true
  end

  it "evaluates the body of the when clause whose array expression includes the case target expression" do
    case 2
    when 1,2: true
    end.should == true
  end

  it "evaluates the body of the when clause whose range expression includes the case target expression" do
    case 5
    when 1..20: true
    end.should == true
  end

  it "evaluates the body of the else clause if no when expressions match the case target expression" do
    case 3
    when 6: false
    else
      true
    end.should == true
  end

  # FIXME - The SyntaxError raises as soon as the block is compiled
  #it "but you can't use else without the when construct" do
  #  should_raise(SyntaxError) do
  #    case 4
  #    else
  #      true
  #    end
  #  end
  #end
  
  it "evaluates the body of the first when clause that is true when no case target expression is given" do
    case
    when 3==3: true
    when 4==4: false
    end.should == true
  end

  # NOTE : This should not work yet, since Onig is not integrated.
  it "evaluates the body of the when clause whose expression is a regex that matches the case target expression" do
    case 'hello'
    when /^hell/: true # mouahahaha
    end.should == true
  end

  it "should evaluate the body of the when clause whose expression is a class using class === case target expression" do
    case 'x'
    when String: true
    end.should == true
  end
end

# * break terminates loop immediately.
# * redo immediately repeats w/o rerunning the condition.
# * next starts the next iteration through the loop.
# * retry restarts the loop, rerunning the condition.

describe "The loop" do
  # loop do
  #   body
  # end
end

describe "The while expression" do
  
  # while bool-expr [do]
  #  body
  # end

  # begin
  #  body
  # end until bool-expr

  # expr while bool-expr

end

describe "The until expression" do
  # until bool-expr [do]
  #  body
  # end

  # begin
  #  body
  # end while bool-expr

  # expr until bool-expr

end

describe "The for expression" do
  # for name[, name]... in expr [do]
  #   body
  # end
  it "iterates over the collection passing each element to the block" do
    j = 0
    for i in 1..3
      j += i
    end
    j.should == 6
  end
end
