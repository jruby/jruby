require 'test/unit'

class TestClassCornerCases < Test::Unit::TestCase

    def setup
        super
        @cls = Class
        @mod = Module
    end
    
    class AllocatedClassSubclass < Class.allocate;end
    class NewClassSubclass < Class.new;end

    def test_class_allocate
        assert_nothing_raised{@cls.allocate}

        # uninitialized class
        assert_raise(TypeError){@cls.allocate.superclass}

        # can't instantiate uninitialized class
        assert_raise(TypeError){@cls.allocate.new}

        # allocator undefined for AllocatedClassSubclass
        assert_raise(TypeError){AllocatedClassSubclass.new}
        assert_raise(TypeError){AllocatedClassSubclass.allocate}
        
        assert_nothing_raised{@cls.allocate.dup}
        assert_nothing_raised{@cls.allocate.clone}
    end

    def test_class_initialize
        @allocated = Class.allocate
        assert_nothing_raised{@allocated.send(:initialize, String)}
        assert_equal(@allocated.superclass, String)

        # already initialized class
        assert_raise(TypeError){@allocated.send(:initialize, String)}
    end

    def test_class_initialize_copy
        @allocated = Class.allocate
        assert_nothing_raised{@allocated.send(:initialize_copy, String)}
        assert_equal(@allocated.superclass, Object)
        @allocated = Class.allocate

        # initialize_copy should take same class object
        assert_raise(TypeError){@allocated.send(:initialize_copy,"")}
    end
    
    def test_class_new
        assert_nothing_raised{@cls.new.allocate}
        assert_nothing_raised{@cls.new(@cls)}
        assert_nothing_raised{@cls.new(@mod).new}

        # wrong instance allocation
        assert_raise(TypeError){@cls.new(@cls).new}
        assert_raise(TypeError){@cls.new(@cls).allocate}

        # superclass must be a class
        assert_raise(TypeError){@cls.new("")}

        # wrong intance allocation
        assert_raise(TypeError){@cls.dup.new("")}
        assert_raise(TypeError){@cls.clone.new("")}
        
        # allocator undefined for #<Class:0x...>
        assert_raise(TypeError){@cls.new(@cls.allocate.dup).new}
        assert_raise(TypeError){@cls.new(@cls.allocate.clone).new}
        
        assert_nothing_raised{NewClassSubclass.new}
        assert_nothing_raised{NewClassSubclass.allocate}        
    end
    
    def test_singleton_class
        o = Object.new
        s = class<<Object;self;end

        # can't create instance of virtual class
        assert_raise(TypeError){s.allocate}
        assert_raise(TypeError){s.new}
        assert_raise(TypeError){eval "class Class < class<<Object;self;end;end"}

        # can't copy singleton class
        assert_raise(TypeError){s.dup}
        assert_raise(TypeError){s.clone}

        # already initialized class (TypeError)
        assert_raise(TypeError){s.send(:initialize)}
        assert_raise(TypeError){s.send(:initialize_copy,"")}

        # can't make subclass of virtual class
        assert_raise(TypeError){@cls.new(s)}
    end

end

class TestConstantCornerCases < Test::Unit::TestCase

    def test_parent_not_changed_on_constant_assignemnt
        assert_equal("M::A", eval( <<-EOF
            $a=Class.new
            module M
                A=$a
            end
            module N
                B=$a
            end
            return N::B.to_s
        EOF
        ))
    end

    def teardown
        $a = nil
    end

end

class TestInheritanceAndReopeningCornerCases < Test::Unit::TestCase

    module AModule;end
    class AClass;end

    def test_class_module_reopen
        assert_nothing_raised{eval "module AModule;end"}
        assert_nothing_raised{eval "class AClass;end"}

        # AModule is not a class
        assert_raise(TypeError){eval "class AModule;end"}

        # AClass is not a module
        assert_raise(TypeError){eval "module AClass;end"}

        # superclass mismatch for class AClass
        assert_raise(TypeError){eval "class AClass<String;end"}
        # superclass must be a Class (Fixnum given)
        assert_raise(TypeError){eval "class AClass<1;end"}
    end
    
    def test_weird_things_allowed_by_parser
        # can't make subclass of virtual class
        assert_raise(TypeError){eval "class AClass<class<<self;self;end;end"}

        assert_nothing_raised{"class C < Class.new(Class.allocate.dup);end"}

        # allocator undefined for C
        assert_nothing_raised{"class C < Class.new(Class.allocate.dup);end;C.new"}
    end

end

class TestMarshalCornerCases < Test::Unit::TestCase
    class AClass;end
    OldClass = AClass
    class AModule;end
    AnInstance = AClass.new
    ASingleton = class<<AnInstance;self;end

    def setup
    end

    def marshal_dump(*obj)
        Marshal.dump obj
    end

    def marshal_load(*str)
        Marshal.load *str
    end

    def marshal_dump_and_load(*obj)
        Marshal.load(Marshal.dump(obj))
    end

    def test_marshal_singleton
        # can't dump anonymous class #<Class:0x28404fc>
        assert_raise(TypeError){marshal_dump Class.new}
        assert_raise(TypeError){marshal_dump Class.allocate}
        
        # can't dump anonymous module #<Module:01xcd2e33>
        assert_raise(TypeError){marshal_dump Module.allocate}
        
        assert_nothing_raised{marshal_dump Object.new}
        assert_nothing_raised{marshal_dump Object.allocate}

        assert_nothing_raised{marshal_dump AClass}

        # a class that that has a singleton without ivars can be dumped
        assert_nothing_raised{marshal_dump AnInstance}

        # singleton class can't be dumped
        assert_raise(TypeError){marshal_dump ASingleton}

        ASingleton.instance_eval{@foo=:blah}

        # singleton can't be dumped
        assert_raise(TypeError){marshal_dump AnInstance}
    end

    class AnotherClass;end
    module AnotherModule;end
    module AnotherModule2;end
    $set_class = lambda{|c|AnotherClass=c}
    $set_module = lambda{|c|AnotherModule=c}
    $set_module2 = lambda{|c|AnotherModule2=c}

    def test_incomplete_type_system_unmarshal_sanity
        stream = marshal_dump(AnotherClass.new)
        #p AnotherClass
        $set_class[nil]

        # TestMarshalCornerCases::AnotherClass does not refer class/module
        assert_raise(TypeError){marshal_load(stream)}

        $set_class[Kernel]

        # TestMarshalCornerCases::AnotherClass does not refer class
        assert_raise(ArgumentError){marshal_load(stream)}

        #$set_class[String]
        # # dump format error
        #assert_raise(ArgumentError){marshal_load(stream)}

        stream = marshal_dump(AnotherModule)
        $set_module[nil]
        # TestMarshalCornerCases::AnotherClass does not refer class/module
        assert_raise(TypeError){marshal_load(stream)}

        stream = marshal_dump(AnotherModule2)
        $set_module2[String]
        # TestMarshalCornerCases::AnotherModule2 does not refer module
        assert_raise(ArgumentError){marshal_load(stream)}
    end
    
    def teardown
        $set_class = $set_module = $set_module2 = nil
    end

end

class TestBuiltinClassesOverwrittenCornerCases < Test::Unit::TestCase

    def test_core_constants_removed_sanity
        # we will have to fire a separate runtime here for this
        # the runtime should either use Constants cached in runtime or use Ruby.getClassFromPath
        # and raise TypeErrors if appropriate
        
        # Object.constants.each{|c|Object.const_set(c, nil) unless ["Object", "Module", "Class"].include? c}
    end

end

class TestUninitializedInstanceSanity < Test::Unit::TestCase
    
    def test_uninitialized_core_classe
        # core class instance sanity after calling plain #allocate without initialization
    end
end
