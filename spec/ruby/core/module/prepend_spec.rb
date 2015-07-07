require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Module#prepend" do
  ruby_version_is "2.1" do
    it "is a public method" do
      Module.should have_public_instance_method(:prepend, true)
    end
  end

  it "calls #prepend_features(self) in reversed order on each module" do
    ScratchPad.record []

    m = Module.new do
      def self.prepend_features(mod)
        ScratchPad << [ self, mod ]
      end
    end

    m2 = Module.new do
      def self.prepend_features(mod)
        ScratchPad << [ self, mod ]
      end
    end

    m3 = Module.new do
      def self.prepend_features(mod)
        ScratchPad << [ self, mod ]
      end
    end

    c = Class.new { prepend(m, m2, m3) }

    ScratchPad.recorded.should == [ [ m3, c], [ m2, c ], [ m, c ] ]
  end

  it "raises a TypeError when the argument is not a Module" do
    lambda { ModuleSpecs::Basic.send(:prepend, Class.new) }.should raise_error(TypeError)
  end

  it "does not raise a TypeError when the argument is an instance of a subclass of Module" do
    lambda { ModuleSpecs::SubclassSpec.send(:prepend, ModuleSpecs::Subclass.new) }.should_not raise_error(TypeError)
  end

  it "does not import constants" do
    m1 = Module.new { A = 1 }
    m2 = Module.new { prepend(m1) }
    m1.constants.should_not include(:A)
  end

  it "imports instance methods" do
    Module.new { prepend ModuleSpecs::A }.instance_methods.should include(:ma)
  end

  it "does not import methods to modules and classes" do
    Module.new { prepend ModuleSpecs::A }.methods.should_not include(:ma)
  end

  it "allows wrapping methods" do
    m = Module.new { def calc(x) super + 3 end }
    c = Class.new { def calc(x) x*2 end }
    c.send(:prepend, m)
    c.new.calc(1).should == 5
  end

  it "also prepends included modules" do
    a = Module.new { def calc(x) x end }
    b = Module.new { include a }
    c = Class.new { prepend b }
    c.new.calc(1).should == 1
  end

  it "prepends multiple modules in the right order" do
    m1 = Module.new { def chain; super << :m1; end }
    m2 = Module.new { def chain; super << :m2; end; prepend(m1) }
    c = Class.new { def chain; [:c]; end; prepend(m2) }
    c.new.chain.should == [:c, :m2, :m1]
  end

  it "includes prepended modules in ancestors" do
    m = Module.new
    Class.new { prepend(m) }.ancestors.should include(m)
  end

  it "reports the prepended module as the method owner" do
    m = Module.new { def meth; end }
    c = Class.new { def meth; end; prepend(m) }
    c.new.method(:meth).owner.should == m
  end

  it "reports the prepended module as the unbound method owner" do
    m = Module.new { def meth; end }
    c = Class.new { def meth; end; prepend(m) }
    c.instance_method(:meth).owner.should == m
    c.public_instance_method(:meth).owner.should == m
  end

  it "causes the prepended module's method to be aliased by alias_method" do
    m = Module.new { def meth; :m end }
    c = Class.new { def meth; :c end; prepend(m); alias_method :alias, :meth }
    c.new.alias.should == :m
  end

  it "sees an instance of a prepended class as kind of the prepended module" do
    m = Module.new
    c = Class.new { prepend(m) }
    c.new.should be_kind_of(m)
  end

  it "keeps the module in the chain when dupping the class" do
    m = Module.new
    c = Class.new { prepend(m) }
    c.dup.new.should be_kind_of(m)
  end

  it "keeps the module in the chain when dupping an intermediate module" do
    m1 = Module.new { def calc(x) x end }
    m2 = Module.new { prepend(m1) }
    c1 = Class.new { prepend(m2) }
    c2 = Class.new { prepend(m2.dup) }
    c1.new.should be_kind_of(m1)
    c2.new.should be_kind_of(m1)
  end

  it "depends on prepend_features to add the module" do
    m = Module.new { def self.prepend_features(mod) end }
    Class.new { prepend(m) }.ancestors.should_not include(m)
  end

  it "adds the module in the subclass chains" do
    parent = Class.new { def chain; [:parent]; end }
    child = Class.new(parent) { def chain; super << :child; end }
    mod = Module.new { def chain; super << :mod; end }
    parent.prepend mod
    parent.ancestors[0,2].should == [mod, parent]
    child.ancestors[0,3].should == [child, mod, parent]

    parent.new.chain.should == [:parent, :mod]
    child.new.chain.should == [:parent, :mod, :child]
  end

  it "inserts a later prepended module into the chain" do
    m1 = Module.new { def chain; super << :m1; end }
    m2 = Module.new { def chain; super << :m2; end }
    c1 = Class.new { def chain; [:c1]; end; prepend m1 }
    c2 = Class.new(c1) { def chain; super << :c2; end }
    c2.new.chain.should == [:c1, :m1, :c2]
    c1.send(:prepend, m2)
    c2.new.chain.should == [:c1, :m1, :m2, :c2]
  end

  it "works with subclasses" do
    m = Module.new do
      def chain
        super << :module
      end
    end

    c = Class.new do
      prepend m
      def chain
        [:class]
      end
    end

    s = Class.new(c) do
      def chain
        super << :subclass
      end
    end

    s.new.chain.should == [:class, :module, :subclass]
  end

  it "throws a NoMethodError when there is no more superclass" do
    m = Module.new do
      def chain
        super << :module
      end
    end

    c = Class.new do
      prepend m
      def chain
        super << :class
      end
    end
    lambda { c.new.chain }.should raise_error(NoMethodError)
  end

  it "calls prepended after prepend_features" do
    ScratchPad.record []

    m = Module.new do
      def self.prepend_features(klass)
        ScratchPad << [:prepend_features, klass]
      end
      def self.prepended(klass)
        ScratchPad << [:prepended, klass]
      end
    end

    c = Class.new { prepend(m) }
    ScratchPad.recorded.should == [[:prepend_features, c], [:prepended, c]]
  end

  it "detects cyclic prepends" do
    lambda {
      module ModuleSpecs::P
        prepend ModuleSpecs::P
      end
    }.should raise_error(ArgumentError)
  end

  it "accepts no-arguments" do
    lambda {
      Module.new do
        prepend
      end
    }.should_not raise_error
  end

  it "returns the class it's included into" do
    m = Module.new
    r = nil
    c = Class.new { r = prepend m }
    r.should == c
  end

  it "clears any caches" do
    module ModuleSpecs::M3
      module PM1
        def foo
          :m1
        end
      end

      module PM2
        def foo
          :m2
        end
      end

      class PC
        prepend PM1

        def get
          foo
        end
      end

      c = PC.new
      c.get.should == :m1

      class PC
        prepend PM2
      end

      c.get.should == :m2
    end
  end

  it "supports super when the module is prepended into a singleton class" do
    module ModuleSpecs::PrependSuperInSingleton
      def included(base)
        super
      end
    end

    module ModuleSpecs::PrependSuperInSingletonModule
      class << self
        prepend ModuleSpecs::PrependSuperInSingleton
      end
    end

    lambda do
      class ModuleSpecs::PrependSuperInSingletonClass
        include ModuleSpecs::PrependSuperInSingletonModule
      end
    end.should_not raise_error
  end
end
