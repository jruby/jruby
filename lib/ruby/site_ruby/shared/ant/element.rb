require 'java'

java_import org.apache.tools.ant.IntrospectionHelper
java_import org.apache.tools.ant.RuntimeConfigurable
java_import org.apache.tools.ant.UnknownElement

# This is really the metadata of the element coupled with the logic for 
# instantiating an instance of an element and evaluating it.  My intention
# is to decouple these two pieces.  This has extra value since we can then
# also make two types of instances for both top-level tasks and for targets
# since we have some conditionals which would then be eliminated
class Element
  attr_reader :name

  def initialize(ant, name, clazz)
    @ant, @name, @clazz = ant, name, clazz
  end

  def call(parent, args={}, &code)
    element = create parent
    assign_attributes element, args
    define_nested_elements element
    code.arity==1 ? code[element] : element.instance_eval(&code) if block_given?
    if parent.respond_to? :get_owning_target # Task
      @ant.project.log "Adding #{name} to #{parent.component_name}", 5
      parent.add_child element
      parent.runtime_configurable_wrapper.add_child element.runtime_configurable_wrapper
    else # Target
      @ant.project.log "Executing #{name}", 5
      element.maybe_configure
      element.execute
    end
  end

  private
  def create(parent) # See ProjectHelper2.ElementHelper
    UnknownElement.new(@name).tap do |e|
      e.project = @ant.project
      e.task_name = @name
    end
  end

  # This also subsumes configureId to only have to traverse args once
  def assign_attributes(instance, args)
    @ant.project.log "instance.task_name #{instance.task_name} #{name}", 5
    wrapper = RuntimeConfigurable.new instance, instance.task_name
    args.each do |key, value|
      # FIXME: Infinite recursion and only single expression expansion
      while value =~ /\$\{([^\}]+)\}/
        value.gsub!(/\$\{[^\}]+\}/, @ant.project.get_property($1).to_s)
      end
      wrapper.set_attribute key, value 
    end
  end

  def define_nested_elements(instance)
    meta_class = class << instance; self; end
    @helper = IntrospectionHelper.get_helper(@ant.project, @clazz)
    @helper.get_nested_element_map.each do |element_name, clazz|
      element = @ant.acquire_element(element_name, clazz)
      meta_class.send(:define_method, element_name) do |*args, &block|
        element.call(instance, *args, &block)
      end
    end
  end
end
