require 'optparse'
require 'jruby'

module JRuby::Compiler
  BAIS = java.io.ByteArrayInputStream
  Mangler = org.jruby.util.JavaNameMangler
  BytecodeCompiler = org.jruby.compiler.impl.StandardASMCompiler
  ASTCompiler = org.jruby.compiler.ASTCompiler
  JavaFile = java.io.File

  def compile_argv(argv)
    basedir = Dir.pwd
    prefix = ""
    target = Dir.pwd
    java = false
    classpath = []

    opt_parser = OptionParser.new("", 24, '  ') do |opts|
      opts.banner = "jrubyc [options] (FILE|DIRECTORY)"
      opts.separator ""

      opts.on("-d", "--dir DIR", "Use DIR as the root of the compiled package and filename") do |dir|
        basedir = dir
      end

      opts.on("-p", "--prefix PREFIX", "Prepend PREFIX to the file path and package. Default is no prefix.") do |pre|
        prefix = pre
      end

      opts.on("-t", "--target TARGET", "Output files to TARGET directory") do |tgt|
        target = tgt
      end

      opts.on("-j", "--java", "Generate normal Java classes to accompany the script") do
        java = true
      end

      opts.on("-c", "--classpath CLASSPATH", "Add a jar to the classpath for building") do |cp|
        classpath.concat cp.split(':')
      end

      opts.parse!(argv)
    end

    if (argv.length == 0)
      raise "No files or directories specified"
    end

    compile_files(argv, basedir, prefix, target, java, classpath)
  end
  module_function :compile_argv

  def compile_files(filenames, basedir = Dir.pwd, prefix = "ruby", target = Dir.pwd, java = false, classpath = [])
    runtime = JRuby.runtime

    unless File.exist? target
      raise "Target dir not found: #{target}"
    end

    files = []

    # The compilation code
    compile_proc = proc do |filename|
      begin
        file = File.open(filename)

        pathname = Mangler.mangle_filename_for_classpath(filename, basedir, prefix)
        puts "Compiling #{filename} to class #{pathname}"

        inspector = org.jruby.compiler.ASTInspector.new

        source = file.read
        node = runtime.parse_file(BAIS.new(source.to_java_bytes), filename, nil)

        inspector.inspect(node)

        asmCompiler = BytecodeCompiler.new(pathname, filename)
        compiler = ASTCompiler.new
        compiler.compile_root(node, asmCompiler, inspector)

        asmCompiler.write_class(JavaFile.new(target))

        if java
          ruby_script = process_script(node, filename)
          ruby_script.classes.each do |cls|
            puts "Generating Java class #{cls.name} to #{cls.name}.java"
            java_src = cls.name + ".java";
            files << java_src

            File.open(java_src, 'w') do |f|
              f.write(cls.to_s)
            end
          end
        end

        0
      rescue Exception
        puts "Failure during compilation of file #{filename}:\n#{$!}"
        puts $!.backtrace
        1
      ensure
        file.close unless file.nil?
      end
    end

    errors = 0
    # Process all the file arguments
    Dir[*filenames].each do |filename|
      unless File.exists? filename
        puts "Error -- file not found: #{filename}"
        errors += 1
        next
      end

      if (File.directory?(filename))
        puts "Compiling all in '#{File.expand_path(filename)}'..."
        Dir.glob(filename + "/**/*.rb").each { |filename|
          errors += compile_proc[filename]
	}
      else
        errors += compile_proc[filename]
      end
    end

    if java
      files_string = files.join(' ')
      jruby_jar, = ['jruby.jar', 'jruby-complete.jar'].select do |jar|
        File.exist? "#{ENV_JAVA['jruby.home']}/lib/#{jar}"
      end
      classpath_string = classpath.size > 0 ? classpath.join(":") : "."
      compile_string = "javac -cp #{ENV_JAVA['jruby.home']}/lib/#{jruby_jar}:#{classpath_string} #{files_string}"
      puts compile_string
      system compile_string
    end

    errors
  end
  module_function :compile_files

  class RubyScript
    BASE_IMPORTS = [
      "org.jruby.Ruby",
      "org.jruby.RubyObject",
      "org.jruby.javasupport.util.RuntimeHelpers",
      "org.jruby.runtime.builtin.IRubyObject",
      "org.jruby.javasupport.JavaUtil",
      "org.jruby.RubyClass"
    ]

    def initialize(script_name, imports = BASE_IMPORTS)
      @classes = []
      @script_name = script_name
      @imports = imports
    end

    attr_accessor :classes, :imports, :script_name

    def add_import(name)
      @imports << name
    end

    def new_class(name, annotations = [])
      cls = RubyClass.new(name, imports, script_name, annotations)
      @classes << cls
      cls
    end

    def to_s
      str = ""
      @classes.each do |cls|
        str << cls.to_s
      end
      str
    end
  end

  class RubyClass
    def initialize(name, imports = [], script_name = nil, annotations = [])
      @name = name
      @imports = imports
      @script_name = script_name
      @methods = []
      @annotations = []
      @interfaces = []
    end

    attr_accessor :methods, :name, :script_name, :annotations, :interfaces

    def new_method(name, java_signature = nil, annotations = [], java_name = nil)
      method = RubyMethod.new(name, java_signature, annotations, java_name)
      methods << method
      method
    end

    def add_interface(ifc)
      @interfaces << ifc
    end

    def interface_string
      if @interfaces.size > 0
        "implements " + @interfaces.join('.')
      else
        ""
      end
    end

    def to_s
      imps_string = imports_string
      ifc_string = interface_string

      class_string = "#{imps_string}\npublic class #{name} extends RubyObject #{ifc_string} {\n"
      class_string << "  private static final Ruby __ruby__ = Ruby.getGlobalRuntime();\n"
      class_string << "  private static final RubyClass __metaclass__;\n"

      static_init = "  static {\n"
      if script_name
        static_init << "    __ruby__.getLoadService().lockAndRequire(\"#{script_name}\");\n"
      end
      static_init << "    RubyClass metaclass = __ruby__.getClass(\"#{name}\");\n"
      static_init << "    metaclass.setClassAllocator(#{name}.class);\n"
      static_init << "    if (metaclass == null) throw new NoClassDefFoundError(\"Could not load Ruby class: #{name}\");\n"
      static_init << "        __metaclass__ = metaclass;\n"
      static_init << "  }\n"

      class_string << static_init

      class_string << <<EOJ
  public #{name}() {
    super(__ruby__, __metaclass__);
  }
EOJ
      
      methods.each do |method|
        class_string << method.to_s
      end
      class_string << "}"

      class_string
    end

    def imports_string
      @imports.map do |import|
        "import #{import};"
      end.join("\n")
    end
  end

  class RubyMethod
    def initialize(name, java_signature = nil, annotations = [], java_name = name)
      @name = name
      @java_signature = java_signature
      @java_name = java_name
      @static = false;
      @args = []
      @annotations = annotations
    end

    attr_accessor :args, :name, :java_signature, :java_name, :static, :annotations

    def format_anno_value(value)
      case value
      when String
        %Q["#{value}"]
      when Fixnum
        value.to_s
      when Array
        "{" + value.map {|v| format_anno_value(v)}.join(',') + "}"
      end
    end

    def to_s
      signature = java_signature
      signature &&= signature.dup
      ret = signature ? signature.shift : 'Object'
      args_string = args.map {|a| "#{signature ? signature.shift : 'Object'} #{a}"}.join(',')
      passed_args = args.map {|a| "ruby_" + a}.join(',')
      passed_args = "," + passed_args if args.size > 0
      conv_string = args.map {|a| '    IRubyObject ruby_' + a + ' = JavaUtil.convertJavaToRuby(__ruby__, ' + a + ');'}.join("\n")
      anno_string = annotations.map {|a| "  @#{a.shift}(" + (a[0] || []).map {|k,v| "#{k} = #{format_anno_value(v)}"}.join(',') + ")\n"}.join
      ret_string = case ret
      when 'void'
        ""
      when 'byte'
        "return (Byte)ruby_result.toJava(byte.class);"
      when 'short'
        "return (Short)ruby_result.toJava(short.class);"
      when 'char'
        "return (Character)ruby_result.toJava(char.class);"
      when 'int'
        "return (Integer)ruby_result.toJava(int.class);"
      when 'long'
        "return (Long)ruby_result.toJava(long.class);"
      when 'float'
        "return (Float)ruby_result.toJava(float.class);"
      when 'double'
        "return (Double)ruby_result.toJava(double.class);"
      when 'boolean'
        "return (Boolean)ruby_result.toJava(boolean.class);"
      else
        "return (#{ret})ruby_result.toJava(#{ret}.class);"
      end

      method_string = <<EOJ
#{anno_string}
  public #{static ? 'static ' : ''}#{ret} #{java_name}(#{args_string}) {
#{conv_string}
    IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), #{static ? '__metaclass__' : 'this'}, \"#{name}\" #{passed_args});
    #{ret_string}
  }
EOJ
      method_string
    end
  end

  class ClassNodeWalker
    include org.jruby.ast.visitor.NodeVisitor
    import org.jruby.ast.NodeType

    def initialize(script_name = nil)
      @script = RubyScript.new(script_name)
      @class_stack = []
      @method_stack = []
      @signature = nil
      @annotations = []
      @name = nil
    end

    attr_accessor :class_stack, :method_stack, :signature, :name, :script, :annotations

    def add_import(name)
      @script.add_import(name)
    end

    def set_signature(name)
      @signature = name
    end

    def set_name(name)
      @name = name
    end

    def prepare_anno_value(value)
      case value.node_type
      when NodeType::STRNODE
        value.value
      when NodeType::ARRAYNODE
        value.child_nodes.map {|v| prepare_anno_value(v)}
      end
    end

    def add_annotation(*child_nodes)
      name = child_nodes[0].name
      args = child_nodes[1]
      if args && args.list_node.size > 0
        anno_args = {}
        child_assocs = args.list_node.child_nodes
        for i in 0...(child_assocs.size / 2)
          key = child_assocs[i * 2]
          value = child_assocs[i * 2 + 1]
          k_name = name_or_value(key)
          v_value = prepare_anno_value(value)

          anno_args[k_name] = v_value
        end
        @annotations << [name, anno_args]
      else
        @annotations << [name]
      end
    end

    def add_interface(*ifc_nodes)
      ifc_nodes.
        map {|ifc| defined?(ifc.name) ? ifc.name : ifc.value}.
        each {|ifc| current_class.add_interface(ifc)}
    end

    def new_class(name)
      cls = @script.new_class(name, @annotations)
      @annotations = []

      class_stack.push(cls)
    end

    def current_class
      class_stack[0]
    end

    def pop_class
      class_stack.pop
      @signature = nil
      @annotations = []
    end

    def new_method(name)
      method = current_class.new_method(name, @signature, @annotations, @name)
      @signature = @name = nil
      @annotations = []

      method_stack.push(method)
    end

    def new_static_method(name)
      method = current_class.new_method(name, @signature, @annotations, @name)
      method.static = true
      @signature = @name = nil
      @annotations = []

      method_stack.push(method)
    end

    def current_method
      method_stack[0]
    end

    def pop_method
      method_stack.pop
    end

    def build_signature(signature_args)
      # assumes hash node
      ary = signature_args.child_nodes[0].child_nodes
      params = ary[0]
      ret = ary[1]

      sig = [(defined? ret.name) ? ret.name : ret.value]
      param_strings = params.child_nodes.map do |param|
        next name_or_value(param)
      end
      sig.concat(param_strings)

      sig
    end

    def build_args_signature(params)
      sig = ["Object"]
      param_strings = params.child_nodes.map do |param|
        if param.respond_to? :type_node
          type_node = param.type_node
          next name_or_value(type_node)
        end
        raise 'unknown signature element: ' + param.to_s
      end
      sig.concat(param_strings)

      sig
    end

    def name_or_value(node)
      return node.name if node.respond_to? :name
      return node.value if node.respond_to? :value
      raise "unknown node :" + node.to_s
    end

    def method_missing(name, *args)
      if name.to_s =~ /^visit/
        node = args[0]
        puts "* entering: #{node.node_type}" if $VERBOSE
        case node.node_type
        when NodeType::ARGSNODE
          # Duby-style arg specification, only pre supported for now
          if node.pre && node.pre.child_nodes.find {|pre_arg| pre_arg.respond_to? :type_node}
            current_method.java_signature = build_args_signature(node.pre)
          end
          node.pre && node.pre.child_nodes.each do |pre_arg|
            current_method.args << pre_arg.name
          end
          node.opt_args && node.opt_args.child_nodes.each do |pre_arg|
            current_method.args << pre_arg.name
          end
          node.post && node.post.child_nodes.each do |post_arg|
            current_method.args << post_arg.name
          end
          if node.rest_arg >= 0
            current_method.args << node.rest_arg_node.name
          end
          if node.block
            current_method.args << node.block.name
          end
        when NodeType::BLOCKNODE
          node.child_nodes.each {|n| n.accept self}
        when NodeType::CALLNODE
          case node.name
          when '+@'
            add_annotation(node.receiver_node,
              *(node.args_node ? node.args_node.child_nodes : []))
          end
        when NodeType::CLASSNODE
          new_class(node.cpath.name)
          node.body_node.accept(self)
          pop_class
        when NodeType::DEFNNODE
          new_method(node.name)
          node.args_node.accept(self)
          pop_method
        when NodeType::DEFSNODE
          new_static_method(node.name)
          node.args_node.accept(self)
          pop_method
        when NodeType::FCALLNODE
          case node.name
          when 'java_import'
            add_import node.args_node.child_nodes[0].value
          when 'java_signature'
            set_signature build_signature(node.args_node.child_nodes[0])
          when 'java_name'
            set_name name_or_value(node.args_node.child_nodes[0])
          when 'java_annotation'
            add_annotation(*node.args_node.child_nodes)
          when 'java_implements'
            add_interface(*node.args_node.child_nodes)
          end
        when NodeType::NEWLINENODE
          node.next_node.accept(self)
        when NodeType::ROOTNODE
          node.body_node.accept(self)
        else
          puts 'unknown: ' + args[0].node_type.to_s
        end
      else
        super
      end
    end
  end

  def process_script(node, script_name = nil)
    walker = ClassNodeWalker.new(script_name)

    node.accept(walker)

    walker.script
  end
  module_function :process_script
end