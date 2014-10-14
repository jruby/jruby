PETests.tests do

  describe "A Symbol" do
    
    example "literal" do
      truffle_assert_constant :foo
    end
    
    example "#== a Symbol" do
      truffle_assert_constant :foo == :foo
    end
    
    example "#!= a Symbol" do
      truffle_assert_constant :foo != :bar
    end

  end

end
