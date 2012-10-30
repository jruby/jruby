describe :range_include, :shared => true do
  it "returns true if other is an element of self" do
    (0..5).send(@method, 2).should == true
    (-5..5).send(@method, 0).should == true
    (-1...1).send(@method, 10.5).should == false
    (-10..-2).send(@method, -2.5).should == true
    ('C'..'X').send(@method, 'M').should == true
    ('C'..'X').send(@method, 'A').should == false
    ('B'...'W').send(@method, 'W').should == false
    ('B'...'W').send(@method, 'Q').should == true
    (0xffff..0xfffff).send(@method, 0xffffd).should == true
    (0xffff..0xfffff).send(@method, 0xfffd).should == false
    (0.5..2.4).send(@method, 2).should == true
    (0.5..2.4).send(@method, 2.5).should == false
    (0.5..2.4).send(@method, 2.4).should == true
    (0.5...2.4).send(@method, 2.4).should == false
  end

  it "compares values using <=>" do
    rng = (1..5)
    m = mock("int")
    m.should_receive(:coerce).and_return([1, 2])
    m.should_receive(:<=>).and_return(1)

    rng.send(@method, m).should be_false
  end
end
