require 'test/minirunit'

class Foo1
    def self.method_added name
        throw Exception.new
    end
    
    test_exception(Exception){def foo;end}
end

class Foo2
    def self.method_removed name
        throw Exception.new
    end
    
    def foo;end

    test_exception(Exception){remove_method :foo}
end

class Foo3
    def self.method_undefined name
        throw Exception.new
    end
    
    def foo;end

    test_exception(Exception){undef_method :foo}
end


class Foo4
    def singleton_method_added name
        throw Exception.new
    end
end

foo4 = Foo4.new
test_exception(Exception){def foo4.foo;end}

class Foo5
    def singleton_method_removed name
        throw Exception.new
    end
end

foo5 = Foo5.new
def foo5.foo;end
test_exception(Exception) do
    class << foo5
        remove_method :foo
    end
end

class Foo6
    def singleton_method_undefined name
        throw Exception.new
    end
end

foo6 = Foo6.new
def foo6.foo;end
test_exception(Exception) do
    class << foo6
        undef_method :foo
    end
end
