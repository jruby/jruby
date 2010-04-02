module JRuby::Compiler
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
      @requires = []
      @package = ""
    end

    attr_accessor :classes, :imports, :script_name, :requires, :package

    def add_import(name)
      @imports << name
    end

    def add_require(require)
      @requires << require
    end

    def new_class(name, annotations = [])
      cls = RubyClass.new(name, imports, script_name, annotations, requires, package)
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
    def initialize(name, imports = [], script_name = nil, annotations = [], requires = [], package = "")
      @name = name
      @imports = imports
      @script_name = script_name
      @methods = []
      @annotations = annotations
      @interfaces = []
      @requires = requires
      @package = package
      @constructor = false;
    end

    attr_accessor :methods, :name, :script_name, :annotations, :interfaces, :requires, :package, :sourcefile

    def new_method(name, java_signature = nil, annotations = [])
      @constructor ||= name == "initialize"
      method = RubyMethod.new(self, name, java_signature, annotations)
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

    def static_init
      return <<JAVA
  static {
#{requires_string}
    RubyClass metaclass = __ruby__.getClass(\"#{name}\");
    metaclass.setClassAllocator(#{name}.class);
    if (metaclass == null) throw new NoClassDefFoundError(\"Could not load Ruby class: #{name}\");
    __metaclass__ = metaclass;
  }
JAVA
    end

    def annotations_string
      annotations.map do |a|
        params = (a[0] || []).map do |k,v|
          "#{k} = #{format_anno_value(v)}"
        end.join(',')

        "@#{a.shift}(#{params})"
      end.join("\n")
    end

    def methods_string
      methods.map(&:to_s).join("\n")
    end

    def requires_string
      if requires.size == 0
        source = File.read script_name
        source_chunks = source.unpack("a32000" * (source.size / 32000 + 1))
        source_chunks.each do |chunk|
          chunk.gsub!(/([\\"])/, '\\\\\1')
          chunk.gsub!("\n", "\\n\" +\n        \"")
        end
        source_line = source_chunks.join("\")\n      .append(\"");

        "    String source = new StringBuilder(\"#{source_line}\").toString();\n    __ruby__.evalScriptlet(source);"
      else
        requires.map do |r|
          "    __ruby__.getLoadService().lockAndRequire(\"#{r}\");"
        end.join("\n")
      end
    end

    def package_string
      if package.empty?
        ""
      else
        "package #{package};"
      end
    end

    def to_s
      class_string = <<JAVA
#{package_string}

#{imports_string}

#{annotations_string}
public class #{name} extends RubyObject #{interface_string} {
  private static final Ruby __ruby__ = Ruby.getGlobalRuntime();
  private static final RubyClass __metaclass__;

#{static_init}

  #{@constructor ? "private" : "public"} #{name}() {
    super(__ruby__, __metaclass__);
  }

#{methods_string}
}
JAVA

      class_string
    end

    def imports_string
      @imports.map do |import|
        "import #{import};"
      end.join("\n")
    end
  end

  class RubyMethod
    def initialize(ruby_class, name, java_signature = nil, annotations = [])
      @ruby_class = ruby_class
      @name = name
      @java_signature = java_signature
      @static = false;
      @args = []
      @annotations = annotations
      @constructor = name == "initialize"
    end

    attr_accessor :args, :name, :java_signature, :static, :annotations

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
      if java_signature
        if java_signature.parameters.size != args.size
          raise "signature and method argument counts do not match"
        end

        ret = java_signature.return_type

        var_names = []
        i = 0;
        args_string = java_signature.parameters.map do |a|
          type = a.type.name
          if a.variable_name
            var_name = a.variable_name
          else
            var_name = args[i]
            i+=1
          end

          var_names << var_name
          "#{type} #{var_name}"
        end.join(', ')

        java_name = java_signature.name

        if ret.void?
          ret_string = "return;"
        else
          ret_string = "return (#{ret.wrapper_name})ruby_result.toJava(#{ret.name}.class);"
        end
      else
        ret = "Object"
        var_names = []
        args_string = args.map{|a| var_names << a; "Object #{a}"}.join(", ")
        java_name = @name
        passed_args = ""
        ret_string = "return ruby_result.toJava(Object.class);"
      end

      passed_args = var_names.map {|a| "ruby_#{a}"}.join(', ')
      passed_args = ', ' + passed_args if args.size > 0

      conv_string = var_names.map {|a| '    IRubyObject ruby_' + a + ' = JavaUtil.convertJavaToRuby(__ruby__, ' + a + ');'}.join("\n")

      anno_string = annotations.map {|a| "  @#{a.shift}(" + (a[0] || []).map {|k,v| "#{k} = #{format_anno_value(v)}"}.join(',') + ")"}.join("\n")

      if @constructor
        method_string = <<JAVA
#{anno_string}
  public #{@ruby_class.name}(#{args_string}) {
    this();
#{conv_string}
    RuntimeHelpers.invoke(__ruby__.getCurrentContext(), #{static ? '__metaclass__' : 'this'}, \"#{name}\" #{passed_args});
  }
JAVA
      else
        method_string = <<EOJ
#{anno_string}
  public #{static ? 'static ' : ''}#{ret} #{java_name}(#{args_string}) {
#{conv_string}
    IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), #{static ? '__metaclass__' : 'this'}, \"#{name}\" #{passed_args});
    #{ret_string}
  }
EOJ
      end

      method_string
    end
  end

  module VisitorBuilder
    def visit(name, &block)
      define_method :"visit_#{name}_node" do |node|
        log "entering: #{node.node_type}"
        with_node(node) do
          instance_eval(&block)
        end
      end
    end

    def visit_default(&block)
      define_method :method_missing do |name, node|
        super unless name.to_s =~ /^visit/

        with_node(node) do
          block.call
        end
      end
    end
  end

  class ClassNodeWalker
    AST = org.jruby.ast

    include AST::visitor::NodeVisitor

    import AST::NodeType
    import org.jruby.parser.JavaSignatureParser
    import java.io.ByteArrayInputStream

    extend VisitorBuilder

    attr_accessor :class_stack, :method_stack, :signature, :script, :annotations, :node

    def initialize(script_name = nil)
      @script = RubyScript.new(script_name)
      @class_stack = []
      @method_stack = []
      @signature = nil
      @annotations = []
      @name = nil
      @node = nil
    end

    def add_import(name)
      @script.add_import(name)
    end

    def set_signature(name)
      @signature = name
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
      name = name_or_value(child_nodes[0])
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
      method = current_class.new_method(name, @signature, @annotations)
      @signature = nil
      @annotations = []

      method_stack.push(method)
    end

    def new_static_method(name)
      method = current_class.new_method(name, @signature, @annotations)
      method.static = true
      @signature = nil
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
      if AST::StrNode === signature_args
        bytes = signature_args.value.to_java_bytes
        sig_node = JavaSignatureParser.parse(ByteArrayInputStream.new(bytes))

        sig_node
      else
        raise "java_signature must take a literal string"
      end
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

    def add_requires(*requires)
      requires.each {|r| @script.add_require(name_or_value(r))}
    end

    def set_package(package)
      @script.package = name_or_value(package)
    end

    def name_or_value(node)
      return node.name if defined? node.name
      return node.value if defined? node.value
      raise "unknown node :" + node.to_s
    end

    def with_node(node)
      begin
        old, @node = @node, node
        yield
      ensure
        @node = old
      end
    end

    def error(message)
      long_message =  "#{node.position}: #{message}"
      raise long_message
    end

    def log(str)
      puts "[jrubyc] #{str}" if $VERBOSE
    end

    visit :args do
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
    end

    visit :call do
      case node.name
      when '+@'
        add_annotation(node.receiver_node,
          *(node.args_node ? node.args_node.child_nodes : []))
      end
    end

    visit :class do
      new_class(node.cpath.name)
      node.body_node.accept(self)
      pop_class
    end

    visit :defn do
      new_method(node.name)
      node.args_node.accept(self)
      pop_method
    end

    visit :defs do
      new_static_method(node.name)
      node.args_node.accept(self)
      pop_method
    end

    visit :fcall do
      case node.name
      when 'java_import'
        add_import node.args_node.child_nodes[0].value
      when 'java_signature'
        set_signature build_signature(node.args_node.child_nodes[0])
      when 'java_annotation'
        add_annotation(*node.args_node.child_nodes)
      when 'java_implements'
        add_interface(*node.args_node.child_nodes)
      when "java_require"
        add_requires(*node.args_node.child_nodes)
      when "java_package"
        set_package(*node.args_node.child_nodes)
      end
    end

    visit :block do
      node.child_nodes.each {|n| n.accept self}
    end

    visit :newline do
      node.next_node.accept(self)
    end

    visit :nil do
    end

    visit :root do
      node.body_node.accept(self)
    end

    visit_default do |node|
      # ignore other nodes
    end
  end

  def process_script(node, script_name = nil)
    walker = ClassNodeWalker.new(script_name)

    node.accept(walker)

    walker.script
  end
  module_function :process_script
end
