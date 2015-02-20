module MetaprogrammingFixtures

  class MethodMissing

    def method_missing(method, *args)
      14
    end

  end

  class ClassWithExistingMethod

    def existing_method(a)
      a
    end

  end

end

PETests.tests do

  example "A call that results in #method_missing" do
    method_missing = MetaprogrammingFixtures::MethodMissing.new
    Truffle::Debug.assert_constant method_missing.does_not_exist
  end

  example "#respond_to? on a method that does exist" do
    object_with_existing_method = MetaprogrammingFixtures::ClassWithExistingMethod.new
    Truffle::Debug.assert_constant object_with_existing_method.respond_to? :existing_method
  end

  example "#send on a method that exists using a symbol" do
    object_with_existing_method = MetaprogrammingFixtures::ClassWithExistingMethod.new
    Truffle::Debug.assert_constant object_with_existing_method.send(:existing_method, 14)
  end

end
