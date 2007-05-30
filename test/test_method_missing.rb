require 'test/unit'

class TestMethodMissing < Test::Unit::TestCase

    class AParent
        def method_missing *args
        end
    end

    class AChild < AParent
        def foo
            super
        end
    end
    
    class AClassWithProtectedAndPrivateMethod
        private
        def a_private_method
        end

        protected
        def a_protected_method
        end
    end


    def test_super_method_missing
        assert_nothing_raised{AChild.new.foo}
    end

    def test_private_method_missing
        assert_raise(NoMethodError){AClassWithProtectedAndPrivateMethod.new.a_private_method}
        begin
            AClassWithProtectedAndPrivateMethod.new.a_private_method
        rescue Exception => e
            assert(e.message =~ /private method/)
        end
    end

    def test_protected_method_missing
        assert_raise(NoMethodError){AClassWithProtectedAndPrivateMethod.new.a_protected_method}
        begin
            AClassWithProtectedAndPrivateMethod.new.a_protected_method
        rescue Exception => e
            assert(e.message =~ /protected method/)
        end
    end
    
    def test_undefined_method_missing
        assert_raise(NoMethodError){AClassWithProtectedAndPrivateMethod.new.a_missing_method}
        begin
            AClassWithProtectedAndPrivateMethod.new.a_missing_method
        rescue Exception => e
            assert(e.message =~ /undefined method/)
        end
    end
    
    def test_no_method_error_args
        begin
            some_method "a", 1
        rescue Exception => e
            assert_equal(e.args, ["a",1])
            assert_equal(e.class, NoMethodError)
        end
    end

    def test_undef_method_and_clone_singleton_class
        s = "a string"
        s.instance_eval do
            (class << self;self;end).class_eval do
                undef_method :length
            end
        end
        assert_raise(NoMethodError){s.clone.length}
        assert_nothing_raised{s.dup.length}
    end
end
