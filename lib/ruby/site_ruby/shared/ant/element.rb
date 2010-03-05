require 'java'

class Ant
  java_import org.apache.tools.ant.IntrospectionHelper
  java_import org.apache.tools.ant.RuntimeConfigurable
  java_import org.apache.tools.ant.UnknownElement

  class UnknownElement
    attr_accessor :ant
    # undef some method names that might collide with ant task/type names
    %w(test fail abort raise exec trap).each {|m| undef_method(m)}
    Object.instance_methods.grep(/java/).each {|m| undef_method(m)}

    def _element(name, args = {}, &block)
      Element.new(ant, name).call(self, args, &block)
    end

    def method_missing(name, *args, &block)
      _element(name, *args, &block)
    end
  end

  # This is really the metadata of the element coupled with the logic for
  # instantiating an instance of an element and evaluating it.  My intention
  # is to decouple these two pieces.  This has extra value since we can then
  # also make two types of instances for both top-level tasks and for targets
  # since we have some conditionals which would then be eliminated
  class Element
    attr_reader :name

    def initialize(ant, name, clazz = nil)
      @ant, @name, @clazz = ant, name, clazz
    end

    def call(parent, args={}, &code)
      element = create_element
      assign_attributes element, args
      define_nested_elements element if @clazz
      code.arity==1 ? code[element] : element.instance_eval(&code) if block_given?
      if parent.respond_to? :add_task # Target
        @ant.project.log "Adding #{name} to #{parent}", 5
        parent.add_task element
      elsif parent.respond_to? :add_child # Task
        @ant.project.log "Adding #{name} to #{parent.component_name}", 5
        parent.add_child element
        parent.runtime_configurable_wrapper.add_child element.runtime_configurable_wrapper
      else # Just run it now
        @ant.project.log "Executing #{name}", 5
        element.owning_target = Target.new.tap {|t| t.name = ""}
        element.maybe_configure
        element.execute
      end
    end

    private
    def create_element # See ProjectHelper2.ElementHandler
      UnknownElement.new(@name).tap do |e|
        e.ant = @ant
        e.project = @ant.project
        e.task_name = @name
        e.location = Ant.location_from_caller
        e.owning_target = @ant.current_target
      end
    end

    # This also subsumes configureId to only have to traverse args once
    def assign_attributes(instance, args)
      @ant.project.log "instance.task_name #{instance.task_name} #{name}", 5
      wrapper = RuntimeConfigurable.new instance, instance.task_name
      args.each do |key, value|
        wrapper.set_attribute key, @ant.project.replace_properties(value)
      end
    end

    def define_nested_elements(instance)
      meta_class = class << instance; self; end
      @helper = IntrospectionHelper.get_helper(@ant.project, @clazz)
      @helper.get_nested_element_map.each do |element_name, clazz|
        element = @ant.acquire_element(element_name, clazz)
        meta_class.send(:define_method, Ant.safe_method_name(element_name)) do |*args, &block|
          element.call(instance, *args, &block)
        end
      end
    end
  end
end
