require File.dirname(__FILE__) + '/../spec_helper'


context 'Single assignment' do
  specify 'Assignment does not modify the lhs, it reassigns its reference' do
    a = 'Foobar'
    b = a
    b = 'Bazquux'
    a.should == 'Foobar'
    b.should == 'Bazquux'
  end

  specify 'Assignment does not copy the object being assigned, just creates a new reference to it' do
    a = []
    b = a
    b << 1
    a.should == [1]
  end

  specify 'If rhs has multiple arguments, lhs becomes an Array of them' do
    a = 1, 2, 3
    a.should == [1, 2, 3]
  end
end

context 'Multiple assignment without grouping or splatting' do
  specify 'An equal number of arguments on lhs and rhs assigns positionally' do
    a, b, c, d = 1, 2, 3, 4
    a.should == 1
    b.should == 2
    c.should == 3
    d.should == 4
  end 

  specify 'If rhs has too few arguments, the missing ones on lhs are assigned nil' do
    a, b, c = 1, 2
    a.should == 1
    b.should == 2
    c.should == nil
  end

  specify 'If rhs has too many arguments, the extra ones are silently not assigned anywhere' do
    a, b = 1, 2, 3
    a.should == 1
    b.should == 2
  end

  specify 'The assignments are done in parallel so that lhs and rhs are independent of eachother without copying' do
    o_of_a, o_of_b = Object.new, Object.new
    a, b = o_of_a, o_of_b
    a, b = b, a
    a.equal?(o_of_b).should == true
    b.equal?(o_of_a).should == true
  end
end

context 'Multiple assignments with splats' do
  specify '* on the lhs has to be applied to the last parameter' do
    should_raise(SyntaxError) { eval 'a, *b, c = 1, 2, 3' }
  end

  specify '* on the lhs collects all parameters from its position onwards as an Array or an empty Array' do
    a, *b = 1, 2
    c, *d = 1
    e, *f = 1, 2, 3
    g, *h = 1, [2, 3]
    *i = 1, [2,3]
    *j = [1,2,3]
    *k = 1,2,3

    a.should == 1
    b.should == [2]
    c.should == 1
    d.should == []
    e.should == 1
    f.should == [2, 3]
    g.should == 1
    h.should == [[2, 3]]
    i.should == [1, [2, 3]]
    j.should == [[1,2,3]]
    k.should == [1,2,3]
  end
end

context 'Multiple assignments with grouping' do
  specify 'A group on the lhs is considered one position and treats its corresponding rhs position like an Array' do
    a, (b, c), d = 1, 2, 3, 4
    e, (f, g), h = 1, [2, 3, 4], 5
    i, (j, k), l = 1, 2, 3
    a.should == 1
    b.should == 2
    c.should == nil
    d.should == 3
    e.should == 1
    f.should == 2
    g.should == 3
    h.should == 5
    i.should == 1
    j.should == 2
    k.should == nil
    l.should == 3
  end

  specify 'rhs cannot use parameter grouping, it is a syntax error' do
    should_raise(SyntaxError) { eval '(a, b) = (1, 2)' }
  end
end


