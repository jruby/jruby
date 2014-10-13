PETests.tests do

  describe "A Fixnum" do
    
    example "literal" do
      truffle_assert_constant 14
      truffle_assert_constant 0xffffffffffff
    end
    
    describe "#+" do
      
      example "a Fixnum" do
        truffle_assert_constant 14 + 2
      end

    end

  end

end
