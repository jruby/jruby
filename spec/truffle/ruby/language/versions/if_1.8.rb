describe "The if expression" do
  it "allows starting then-body on the same line if colon is used" do
    if true: 123
    else
      456
    end.should == 123
  end

  it "allows both then- and else-bodies start on the same line (with colon after expression)" do
    if false: 123
    else 456
    end.should == 456
  end

  it "allows ':' after expression when then-body is on the next line" do
    if true:
      123
    end.should == 123

    if true; 123; end.should == 123
  end

  it "allows then-body on the same line separated with ':'" do
    if true: 123
    end.should == 123

    if true: 123; end.should == 123
  end

  it "returns nil when then-body on the same line separated with ':' and expression is false" do
    if false: 123
    end.should == nil

    if false: 123; end.should == nil
  end

  it "returns nil when then-body separated by ':' is empty and expression is true" do
    if true:
    end.should == nil

    if true: ; end.should == nil
  end

  it "returns nil when then-body separated by ':', expression is false and no else part" do
    if false:
    end.should == nil

    if false: ; end.should == nil
  end

  it "evaluates then-body when then-body separated by ':', expression is true and else part is present" do
    if true: 123
    else 456
    end.should == 123

    if true: 123; else 456; end.should == 123
  end

  it "evaluates else-body when then-body separated by ':' and expression is false" do
    if false: 123
    else 456
    end.should == 456

    if false: 123; else 456; end.should == 456
  end

  it "allows elsif-body at the same line separated by ':' or 'then'" do
    if false then 123
    elsif false: 234
    elsif true then 345
    elsif true: 456
    end.should == 345

    if false: 123; elsif false then 234; elsif true: 345; elsif true then 456; end.should == 345
  end
end
