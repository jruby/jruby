PETests.tests do

  describe "A Fixnum" do
    
    example "literal" do
      truffle_assert_constant 14
      truffle_assert_constant 0xffffffffffff
    end

  end

end
