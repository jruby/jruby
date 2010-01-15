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

      opts.parse!(argv)
    end

    if (argv.length == 0)
      raise "No files or directories specified"
    end

    compile_files(argv, basedir, prefix, target, java)
  end
  module_function :compile_argv

  def compile_files(filenames, basedir = Dir.pwd, prefix = "ruby", target = Dir.pwd, java = false)
    runtime = JRuby.runtime

    unless File.exist? target
      raise "Target dir not found: #{target}"
    end

    files = []

    # The compilation code
    compile_proc = proc do |filename|
      begin
        file = File.open(filename)

        classpath = Mangler.mangle_filename_for_classpath(filename, basedir, prefix)
        puts "Compiling #{filename} to class #{classpath}"

        inspector = org.jruby.compiler.ASTInspector.new

        source = file.read
        node = runtime.parse_file(BAIS.new(source.to_java_bytes), filename, nil)

        inspector.inspect(node)

        asmCompiler = BytecodeCompiler.new(classpath, filename)
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
      compile_string = "javac -cp #{ENV_JAVA['jruby.home']}/lib/jruby.jar:. #{files_string}"
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

    def new_class(name)
      cls = RubyClass.new(name, imports, script_name)
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
    def initialize(name, imports = [], script_name = nil)
      @name = name
      @imports = imports
      @script_name = script_name
      @methods = []
    end

    attr_accessor :methods, :name, :script_name

    def new_method(name, java_signature = nil, java_name = nil)
      method = RubyMethod.new(name, java_signature, java_name)
      methods << method
      method
    end

    def to_s
      class_string = imports_string

      class_string << "public class #{name} extends RubyObject {\n"
      class_string << "  private static final Ruby __ruby__ = Ruby.getGlobalRuntime();\n"
      class_string << "  private static final RubyClass __metaclass__;\n"

      static_init = "  static {\n"
      if script_name
        static_init << "    __ruby__.getLoadService().lockAndRequire(\"#{script_name}\");\n"
      end
      static_init << "    RubyClass metaclass = __ruby__.getClass(\"#{name}\");\n"
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
    def initialize(name, java_signature = nil, java_name = nil)
      @name = name
      @java_signature = java_signature
      @java_name = java_name
      @static = false;
      @args = []
    end

    attr_accessor :args, :name, :java_signature, :java_name, :static

    def to_s
      signature = java_signature
      signature &&= signature.dup
      ret = signature ? signature.shift : 'Object'
      args_string = args.map {|a| "#{signature ? signature.shift : 'Object'} #{a}"}.join(',')
      passed_args_string = args.map {|a| "ruby_" + a}.join(',')
      conv_string = args.map {|a| '    IRubyObject ruby_' + a + ' = JavaUtil.convertJavaToRuby(__ruby__, ' + a + ');'}.join("\n")
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
  public #{static ? 'static ' : ''}#{ret} #{name}(#{args_string}) {
#{conv_string}
    IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), #{static ? '__metaclass__' : 'this'}, \"#{name}\", #{passed_args_string});
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
      @name = nil
    end

    attr_accessor :class_stack, :method_stack, :signature, :name, :script

    def add_import(name)
      @script.add_import(name)
    end

    def set_signature(name)
      @signature = name
    end

    def set_name(name)
      @name = name
    end

    def new_class(name)
      cls = @script.new_class(name)

      class_stack.push(cls)
    end

    def current_class
      class_stack[0]
    end

    def pop_class
      class_stack.pop
      @signature = nil
    end

    def new_method(name)
      method = current_class.new_method(name, @signature, @name)
      @signature = @name = nil

      method_stack.push(method)
    end

    def new_static_method(name)
      method = current_class.new_method(name, @signature, @name)
      method.static = true
      @signature = @name = nil

      method_stack.push(method)
    end

    def current_method
      method_stack[0]
    end

    def pop_method
      method_stack.pop
    end

    def build_signature(args)
      # assumes hash node
      ary = args.child_nodes[0].child_nodes
      params = ary[0]
      ret = ary[1]

      sig = [(defined? ret.name) ? ret.name : ret.value]
      param_strings = params.child_nodes.map do |param|
        next param.name if defined? param.name
        next param.value if defined? param.value
        raise 'unknown signature element: ' + param.to_s
      end
      sig.concat(param_strings)

      sig
    end

    def method_missing(name, *args)
      if name.to_s =~ /^visit/
        node = args[0]
        puts "* entering: #{node.node_type}" if $VERBOSE
        case node.node_type
        when NodeType::ARGSNODE
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
            set_name node.args_node.child_nodes[0].value
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