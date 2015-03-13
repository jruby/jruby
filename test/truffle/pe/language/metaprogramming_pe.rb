# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

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

# all fail due to calls not being inlined
tagged_example "MetaprogrammingFixtures::MethodMissing.new.does_not_exist"
tagged_example "MetaprogrammingFixtures::ClassWithExistingMethod.new.respond_to?(:existing_method)"
tagged_example "MetaprogrammingFixtures::ClassWithExistingMethod.new.send(:existing_method, 14)"
