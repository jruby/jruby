module JavaCode
  class Flag
    def initialize(code)
      @code = code
    end

    def to_s
      @code
    end
  end

  PRIVATE = Flag.new("private")
  PROTECTED = Flag.new("protected")
  PUBLIC =Flag.new("public")

  ABSTRACT = Flag.new("abstract")
  FINAL = Flag.new("final")
  STATIC = Flag.new("static")

  class JClass
    attr_accessor :visible
    attr_reader :flags

    def initialize(name)
      @name = name
      @visible = PUBLIC
      @flags = []
      @methods = []
      @classes = []
    end

    def add_method(method)
      @methods << method
      self
    end

    def add_class(new_class)
      @classes << new_class
      self
    end

    def <<(object)
      add_method (object) if object.instance_of?(JMethod)
      add_class (object) if object.instance_of?(JClass)
      self
    end

    def to_s
      code = "#@visible #{@flags.join(" ")} class #@name {"
      @methods.each { |method|
	code << "\n" << method
      }
      @classes.each { |aclass|
	code << "\n" << aclass
      }
      code << "}"
    end

    def to_str
      to_s
    end
  end

  class JMethod
    attr_accessor :visible, :return_type, :parameter
    attr_reader :flags, :local
    
    def initialize(name)
      @name = name
      @visible = PUBLIC
      @return_type = "void"
      @parameter = ""
      @flags = []
      @local = []
    end

    def <<(code)
      @body = "" if @body.nil?
      @body << code
      self
    end

    def write(code)
      self << code
    end

    def to_s
      code = "#@visible #{@flags.join(" ")} #@return_type #@name(#@parameter) {\n"
      @local.uniq.each { |item|
	code << "item" << ";\n"
      }
      code << @body
      code << "}\n"
      code
    end

    def to_str
      to_s
    end
  end
end
