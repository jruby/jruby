PETests.tests do

  describe "A Float" do
    
    example "literal" do
      truffle_assert_constant 14.2
    end
    
    describe "#+" do
      
      example "a Fixnum" do
        truffle_assert_constant 14.0 + 2
      end
      
      counter_example "a Bignum" do
        truffle_assert_constant 14.0 + 0xfffffffffffffffffffffffffffffff
      end
      
      example "a Float" do
        truffle_assert_constant 14.0 + 2.0
      end
      
      counter_example "rand" do
        truffle_assert_constant 14.0 + rand
      end

    end
    
    describe "#*" do
      
      example "a Fixnum" do
        truffle_assert_constant 14.0 * 2
      end
      
      counter_example "a Bignum" do
        truffle_assert_constant 14.0 * 0xfffffffffffffffffffffffffffffff
      end
      
      example "a Float" do
        truffle_assert_constant 14.0 * 2.0
      end
      
      counter_example "rand" do
        truffle_assert_constant 14.0 * rand
      end

    end
    
    describe "#/" do
      
      example "a Fixnum" do
        truffle_assert_constant 14.0 / 2
      end
      
      counter_example "a Bignum" do
        truffle_assert_constant 14.0 / 0xfffffffffffffffffffffffffffffff
      end
      
      example "a Float" do
        truffle_assert_constant 14.0 / 2.0
      end
      
      counter_example "rand" do
        truffle_assert_constant 14.0 / rand
      end

    end

  end

end
