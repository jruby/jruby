require 'java'
require 'ant/element'
require 'ant/target'

java_import org.apache.tools.ant.ComponentHelper
java_import org.apache.tools.ant.DefaultLogger
java_import org.apache.tools.ant.Location
java_import org.apache.tools.ant.Project
java_import org.apache.tools.ant.ProjectHelper
java_import org.apache.tools.ant.Target

class Ant
  attr_reader :project, :log, :location
  attr_accessor :current_target

  def initialize(options={}, &block)
    @options = options
    @location = Location.new(caller.detect{|el| el !~ /^#{__FILE__}/}.split(/:/).first)
    @project = create_project options
    @current_target = nil
    initialize_elements
    handle_argv unless options[:run] == false || Ant.run || @location.file_name != $0
  end

  def create_project(options)
    # If we are calling into a rakefile from ant then we already have a project to use
    return $project if defined?($project) && $project

    output_level = options.delete(:output_level) || 2

    Project.new.tap do |p|
      p.init
      p.add_build_listener(DefaultLogger.new.tap do |log|
        log.output_print_stream = java.lang.System.out
        log.error_print_stream = java.lang.System.err
        log.emacs_mode = true
        log.message_output_level = output_level
        @log = log
      end)
      helper = ProjectHelper.getProjectHelper
      helper.import_stack.add(Object.new)
      p.addReference(ProjectHelper::PROJECTHELPER_REFERENCE, helper)
      options.each_pair {|k,v| p.send("set_#{k}", v) }
    end
  end

  def properties
    @project.properties
  end

  # Add a target (two forms)
  # 1. Execute a block as a target: add_target "foo-target" { echo :message => "I am cool" }
  # 2. Execute a rake task as a target: add_target Rake.application["default"]
  def add_target(*options, &block)
    target = options.first.respond_to?(:name) ? RakeTarget.new(self, options.first) : BlockTarget.new(self, *options, &block)
    @project.add_target target
  end
  alias target add_target

  def [](name)
    if @project.targets.containsKey(name)
      TargetWrapper.new(@project, name)
    else
      MissingWrapper.new(@project, name)
    end
  end

  def execute_target(name)
    self[name].execute
  end

  def execute_default
    @project.execute_target(@project.default_target)
  end

  def project_help
    max_width = @project.targets.keys.max {|a,b| a.length <=> b.length}.length
    @project.targets.values.select {|t|
      t.description
    }.sort{|a,b|
      a.name <=> b.name
    }.map {|t|
      "%-#{max_width}s - %s" % [t.name, t.description]
    }.join("\n")
  end


  # We generate top-level methods for all default data types and task definitions for this instance
  # of ant.  This eliminates the need to rely on method_missing.
  def initialize_elements
    @elements = {}
    @helper = ComponentHelper.get_component_helper @project
    generate_children @project.data_type_definitions
    generate_children @project.task_definitions
  end

  # All elements (including nested elements) are registered so we can access them easily.
  def acquire_element(name, clazz)
    element = @elements[name + clazz.to_s]
    return element if element

    # Not registered in ant's type registry for this project (nested el?)
    unless @helper.get_definition(name)
      @project.log "Adding #{name} -> #{clazz.inspect}", 5
      @helper.add_data_type_definition(name, clazz)
    end

    @elements[name + clazz.to_s] = :give_it_something_to_prevent_endless_recursive_defs
    @elements[name + clazz.to_s] = Element.new(self, name, clazz)
  end

  def _element(name, *args, &block)
#     raise "unknown element #{name}!" unless @helper.get_definition(name)
    element = acquire_element(name, nil)
    element.call(@current_target, *args, &block)
  end

  def run(*targets)
    if targets.length > 0
      targets.each {|t| execute_target(t) }
    else
      execute_default
    end
  end

  private
  def generate_children(collection)
    collection.each do |name, clazz|
      element = acquire_element(name, clazz)
      self.class.send(:define_method, Ant.safe_method_name(name)) do |*a, &b|
        element.call(@current_target, *a, &b)
      end
    end
  end

  def handle_argv
    argv = ARGV
    properties = []
    targets = []
    argv.each {|a| a =~ /^-D/ ? properties << a[2..-1] : targets << a }
    properties.each do |p|
      key, value = p.split('=', 2)
      value ||= "true"
      @project.set_user_property(key, value)
    end
    at_exit do
      begin
        run(*targets) unless Ant.run
      rescue => e
        warn e.message
        exit 1
      end
    end
  end

  class << self
    attr_accessor :run

    def safe_method_name(element_name)
      if element_name =~ /\A(and|or|not|do|end|if|else)\z/m
        "_#{element_name}"
      else
        element_name
      end
    end

    def ant(options={}, &code)
      if options.respond_to? :to_hash
        @ant ||= Ant.new options.to_hash
        code.arity==1 ? code[@ant] : @ant.instance_eval(&code) if block_given?
        @ant
      else
        options = options.join(" ") if options.respond_to? :to_ary
        sh "ant #{options.to_s}"  # FIXME: Make this more secure if using array form
      end
    end
  end
end

# This method has three different uses:
#
# 1. Call an ant task or type directly:
#      task :compile do # Rake task
#        ant.javac { }  # Look I am calling an ant task
#      end
# 2. Provide a block to provide an impromptu ant session
#      ant do
#        javac {}       # Everything executes as if in an executing ant target
#      end
# 3. Provide arguments to execute ant as it's own build
#      ant '-f my_build.xml my_target1'
#
#      Additionally this may be passed in array format if you are worried about injection:
#
#      args = ['-f', 'my_build.xml', 'my_target1']
#      ant args
#
def ant(*args, &block)
  Ant.ant(*args, &block)
end

def ant_import(filename = 'build.xml')
  ant = Ant.ant

  ProjectHelper.configure_project ant.project, java.io.File.new(filename)

  ant.project.targets.each do |target_name, target|
    name = Rake.application.lookup(target_name) ? "ant_" + target_name : target_name

    task(name) { target.project.execute_target(target_name) }
  end
end
