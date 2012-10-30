describe "An instance method definition with a splat" do
  it "creates a method that can be invoked with an inline hash argument" do
    def foo(a,b,*c); [a,b,c] end

    foo('abc', 'rbx' => 'cool', 'specs' => 'fail sometimes', 'oh' => 'shit', *[789, 'yeah']).
      should ==
      ['abc', { 'rbx' => 'cool', 'specs' => 'fail sometimes', 'oh' => 'shit'}, [789, 'yeah']]
  end

  it "creates a method that can be invoked with an inline hash and a block" do
    def foo(a,b,*c,&d); [a,b,c,yield] end

    foo('abc', 'rbx' => 'cool', 'specs' => 'fail sometimes', 'oh' => 'shit', *[789, 'yeah']) { 3 }.
      should ==
      ['abc', { 'rbx' => 'cool', 'specs' => 'fail sometimes', 'oh' => 'shit'}, [789, 'yeah'], 3]

    foo('abc', 'rbx' => 'cool', 'specs' => 'fail sometimes', *[789, 'yeah']) do 3 end.should ==
      ['abc', { 'rbx' => 'cool', 'specs' => 'fail sometimes' }, [789, 'yeah'], 3]

    l = lambda { 3 }

    foo('abc', 'rbx' => 'cool', 'specs' => 'fail sometimes', *[789, 'yeah'], &l).should ==
      ['abc', { 'rbx' => 'cool', 'specs' => 'fail sometimes' }, [789, 'yeah'], 3]
  end
end
