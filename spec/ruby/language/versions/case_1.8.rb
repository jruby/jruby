describe "The 'case'-construct" do
  it "returns the statement following ':'" do
    case "a"
      when "a": 'foo'
      when "b": 'bar'
    end.should == 'foo'
  end

  it "allows mixing ':' and 'then'" do
    case "b"
      when "a": 'foo'
      when "b" then 'bar'
    end.should == 'bar'
  end
end
