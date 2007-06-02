require File.dirname(__FILE__) + '/../../spec_helper'

# TODO: break, redo, next, retry

# until bool-expr [do]
#   body
# end
# 
# begin
#   body
# end while bool-expr
# 
# expr until bool-expr
describe "The until expression" do
  it "should run while the expression is false" do
    i = 0
    until i > 9
      i += 1
    end

    i.should == 10
  end
  
  it "should optionally take a 'do' after the expression" do
    i = 0
    until i > 9 do
      i += 1
    end
    
    i.should == 10
  end
  
  it "should allow body begin on the same line if do is used" do
    i = 0
    until i > 9 do i += 1
    end
    
    i.should == 10
  end
  
  it "should execute code in containing variable scope" do
    i = 0
    until i == 1
      a = 123
      i = 1
    end
    
    a.should == 123
  end
  
  it "should execute code in containing variable scope with 'do'" do
    i = 0
    until i == 1 do
      a = 123
      i = 1
    end
    
    a.should == 123
  end
  
  it "should return nil if ended when condition became true" do
    i = 0
    while i > 9
      i += 1
    end.should == nil
  end
  
  it "should stop running body if interrupted by break" do
    i = 0
    until i > 9
      i += 1
      break if i > 5
    end
    i.should == 6
  end
  
  it "should return value passed to break if interrupted by break" do
    until false
      break 123
    end.should == 123
  end
  
  it "should return nil if interrupted by break with no arguments" do
    until false
      break
    end.should == nil
  end

  it "should skip to end of body with next" do
    a = []
    i = 0
    until (i+=1)>=5
      next if i==3
      a << i
    end
    a.should == [1, 2, 4]
  end

  it "should restart current iteration without reevaluating condition with redo" do
    a = []
    i = 0
    j = 0
    until (i+=1)>=3
      a << i
      j+=1
      redo if j<3
    end
    a.should == [1, 1, 1, 2]
  end
end
  
describe "The until modifier" do
  it "should run preceding statement while the condition is false" do
    i = 0
    i += 1 until i > 9
    i.should == 10
  end
  
  it "should evaluate condition before statement execution" do
    a = []
    i = 0
    a << i until (i+=1) >= 3
    a.should == [1, 2]
  end
  
  it "should not run preceding statement if the condition is true" do
    i = 0
    i += 1 until true
    i.should == 0
  end
  
  it "should return nil if ended when condition became true" do
    i = 0
    (i += 1 until i>9).should == nil
  end
  
  it "should return value passed to break if interrupted by break" do
    (break 123 until false).should == 123
  end
  
  it "should return nil if interrupted by break with no arguments" do
    (break until false).should == nil
  end

  it "should skip to end of body with next" do
    a = []
    i = 0
    lambda { next if i==3; a << i }.call until (i+=1)>=5
    a.should == [1, 2, 4]
  end

  it "should restart current iteration without reevaluating condition with redo" do
    a = []
    i = 0
    j = 0
    lambda { a << i; j+=1; redo if j<3 }.call until (i+=1)>=3
    a.should == [1, 1, 1, 2]
  end
end


describe "The until modifier with begin .. end block" do
  
  it "should run block while the expression is false" do
    i = 0
    begin
      i += 1
    end until i > 9
    
    i.should == 10
  end
  
  it "should stop running block if interrupted by break" do
    i = 0
    begin
      i += 1
      break if i > 5
    end until i > 9
    
    i.should == 6
  end
  
  it "should return value passed to break if interrupted by break" do
    (begin; break 123; end until false).should == 123
  end
  
  it "should return nil if interrupted by break with no arguments" do
    (begin; break; end until false).should == nil
  end
  
  it "should run block at least once (even if the expression is true)" do
    i = 0
    begin
      i += 1
    end until true
    
    i.should == 1
  end

  it "should evaluate condition after block execution" do
    a = []
    i = 0
    begin
      a << i
    end until (i+=1)>=5
    a.should == [0, 1, 2, 3, 4]
  end

  it "should skip to end of body with next" do
    a = []
    i = 0
    begin
      next if i==3
      a << i
    end until (i+=1)>=5
    a.should == [0, 1, 2, 4]
  end

  it "should restart current iteration without reevaluting condition with redo" do
    a = []
    i = 0
    j = 0
    begin
      a << i
      j+=1
      redo if j<3
    end until (i+=1)>=3
    a.should == [0, 0, 0, 1, 2]
  end
end
