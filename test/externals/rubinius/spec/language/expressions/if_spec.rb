require File.dirname(__FILE__) + '/../../spec_helper'

describe "The if expression" do
  it "should evaluate body if expression is true" do
    a = []
    if true
      a << 123
    end
    a.should == [123]
  end

  it "should not evaluate body if expression is false" do
    a = []
    if false
      a << 123
    end
    a.should == []
  end

  it "should not evaluate else-body if expression is true" do
    a = []
    if true
      a << 123
    else
      a << 456
    end
    a.should == [123]
  end

  it "should evaluate only else-body if expression is false" do
    a = []
    if false
      a << 123
    else
      a << 456
    end
    a.should == [456]
  end

  it "should return result of then-body evaluation if expression is true" do
    if true
      123
    end.should == 123
  end

  it "should return result of last statement in then-body if expression is true" do
    if true
      'foo'
      'bar'
      'baz'
    end.should == 'baz'
  end

  it "should return result of then-body evaluation if expression is true and else part is present" do
    if true
      123
    else
      456
    end.should == 123
  end

  it "should return result of else-body evaluation if expression is false" do
    if false
      123
    else
      456
    end.should == 456
  end

  it "should return nil if then-body is empty and expression is true" do
    if true
    end.should == nil
  end

  it "should return nil if then-body is empty, expression is true and else part is present" do
    if true
    else
      456
    end.should == nil
  end

  it "should return nil if then-body is empty, expression is true and else part is empty" do
    if true
    else
    end.should == nil
  end

  it "should return nil if else-body is empty and expression is false" do
    if false
      123
    else
    end.should == nil
  end

  it "should return nil if else-body is empty, expression is false and then-body is empty" do
    if false
    else
    end.should == nil
  end

  it "should consider expression with nil result as false" do
    if nil
      123
    else
      456
    end.should == 456
  end

  it "should consider non-nil and non-boolean object in expression result as true" do
    if Object.new
      123
    else
      456
    end.should == 123
  end

  it "should consider zero integer in expression result as true" do
    if 0
      123
    else
      456
    end.should == 123
  end

  it "should allow starting then-body on the same line if colon is used" do
    if true: 123
    else
      456
    end.should == 123
  end

  it "should allow starting else-body on the same line" do
    if false
      123
    else 456
    end.should == 456
  end

  it "should allow both then- and else-bodies start on the same line (with colon after expression)" do
    if false: 123
    else 456
    end.should == 456
  end

  it "should evaluate subsequent elsif statements and execute body of first matching" do
    if false
      123
    elsif false
      234
    elsif true
      345
    elsif true
      456
    end.should == 345
  end

  it "should evaluate else-body if no if/elsif statements match" do
    if false
      123
    elsif false
      234
    elsif false
      345
    else
      456
    end.should == 456
  end

  it "should allow ':' after expression when then-body is on the next line" do
    if true:
      123
    end.should == 123

    if true; 123; end.should == 123
  end

  it "should allow 'then' after expression when then-body is on the next line" do
    if true then
      123
    end.should == 123

    if true then ; 123; end.should == 123
  end

  it "should allow then-body on the same line separated with ':'" do
    if true: 123
    end.should == 123

    if true: 123; end.should == 123
  end

  it "should allow then-body on the same line separated with 'then'" do
    if true then 123
    end.should == 123

    if true then 123; end.should == 123
  end

  it "should return nil when then-body on the same line separated with ':' and expression is false" do
    if false: 123
    end.should == nil

    if false: 123; end.should == nil
  end

  it "should return nil when then-body on the same line separated with 'then' and expression is false" do
    if false then 123
    end.should == nil

    if false then 123; end.should == nil
  end

  it "should return nil when then-body separated by ':' is empty and expression is true" do
    if true:
    end.should == nil

    if true: ; end.should == nil
  end

  it "should return nil when then-body separated by 'then' is empty and expression is true" do
    if true then
    end.should == nil

    if true then ; end.should == nil
  end

  it "should return nil when then-body separated by ':', expression is false and no else part" do
    if false:
    end.should == nil

    if false: ; end.should == nil
  end

  it "should return nil when then-body separated by 'then', expression is false and no else part" do
    if false then
    end.should == nil

    if false then ; end.should == nil
  end

  it "should evaluate then-body when then-body separated by ':', expression is true and else part is present" do
    if true: 123
    else 456
    end.should == 123

    if true: 123; else 456; end.should == 123
  end

  it "should evaluate then-body when then-body separated by 'then', expression is true and else part is present" do
    if true then 123
    else 456
    end.should == 123

    if true then 123; else 456; end.should == 123
  end

  it "should evaluate else-body when then-body separated by ':' and expression is false" do
    if false: 123
    else 456
    end.should == 456

    if false: 123; else 456; end.should == 456
  end

  it "should evaluate else-body when then-body separated by 'then' and expression is false" do
    if false then 123
    else 456
    end.should == 456

    if false then 123; else 456; end.should == 456
  end

  it "should allow elsif-body at the same line separated by ':' or 'then'" do
    if false then 123
    elsif false: 234
    elsif true then 345
    elsif true: 456
    end.should == 345

    if false: 123; elsif false then 234; elsif true: 345; elsif true then 456; end.should == 345
  end
end

describe "The postfix if form" do
  it "should evaluate statement if expression is true" do
    a = []
    a << 123 if true
    a.should == [123]
  end

  it "should not evaluate statement if expression is false" do
    a = []
    a << 123 if false
    a.should == []
  end

  it "should return result of expression if value is true" do
    (123 if true).should == 123
  end

  it "should return nil if expression is false" do
    (123 if false).should == nil
  end

  it "should consider nil expression as false" do
    (123 if nil).should == nil
  end

  it "should consider non-nil object as true" do
    (123 if Object.new).should == 123
  end

  it "should evaluate then-body in containing scope" do
    a = 123
    if true
      b = a+1
    end
    b.should == 124
  end

  it "should evaluate else-body in containing scope" do
    a = 123
    if false
      b = a+1
    else
      b = a+2
    end
    b.should == 125
  end

  it "should evaluate elsif-body in containing scope" do
    a = 123
    if false
      b = a+1
    elsif false
      b = a+2
    elsif true
      b = a+3
    else
      b = a+4
    end
    b.should == 126
  end
end
