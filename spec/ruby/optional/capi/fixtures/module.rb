class CApiModuleSpecs
  class A
    X = 1
  end

  class B < A
    Y = 2
  end

  class C
    Z = 3
  end

  module M
  end

  class Super
  end

  autoload :ModuleUnderAutoload, "#{extension_path}/module_under_autoload_spec"
  autoload :RubyUnderAutoload, File.expand_path('../module_autoload', __FILE__)

end
