require File.expand_path('../spec_helper', __FILE__)
require 'ant'
require 'tmpdir'

class Ant
  module RSpec
    module AntExampleGroup
      def self.included(example_group)
        example_group.after :each do
          Ant.instance_eval do
            instance_variable_set "@ant", nil
          end
        end
      end

      # Allows matching task structure with a nested hash as follows.
      #
      # { :_name => "jar", :destfile => "spec-test.jar", :compress => "true", :index => "true",
      #   :_children => [
      #     { :_name => "fileset", :dir => "build" }
      #   ]}
      #
      # would match the following:
      #
      # jar :destfile => "spec-test.jar", :compress => "true", :index => "true" do
      #   fileset :dir => "build"
      # end
      #
      class TaskStructureMatcher
        def initialize(hash, configure = false)
          @expected = hash
          @configure = configure
        end

        def description
          "have the specified #{(@configure ? 'configured' : 'element')} structure"
        end

        def matches?(actual)
          @actual = actual
          result = true
          tasks = actual.defined_tasks
          if Hash === @expected && tasks.length != 1 || tasks.length != @expected.length
            @message = "task list length different"
            return false
          end
          @expected.each_with_index do |h,i|
            tasks[i].maybe_configure if @configure
            result &&= match_wrapper(h, tasks[i].wrapper)
          end
          result
        end

        def match_wrapper(hash, wrapper, name = nil)
          hname = hash[:_name] ? hash[:_name] : '<?>'
          if name
            name = "#{name} => #{hname}"
          else
            name = hname
          end

          # name
          if hash[:_name] && hash[:_name] != wrapper.element_tag
            @message = "name different: expected #{hash[:_name].inspect} but was #{wrapper.element_tag.inspect}"
            return false
          end

          # proxy class name
          if hash[:_type] && (wrapper.proxy.nil? || hash[:_type] != wrapper.proxy.java_class.name)
            @message = "type different: expected #{hash[:_type]} but was #{wrapper.proxy.java_class.name}"
            return false
          end

          # attributes
          hash.keys.select {|k| k.to_s !~ /^_/}.each do |k|
            unless wrapper.attribute_map.containsKey(k.to_s)
              @message = "'#{name} => #{k}' attribute missing"
              return false
            end

            if hash[k] != wrapper.attribute_map[k.to_s]
              @message = "'#{name} => #{k}' attribute different: expected #{hash[k].inspect} but was #{wrapper.attribute_map[k.to_s].inspect}"
              return false
            end
          end

          # children, recursively
          children = wrapper.children.to_a
          (hash[:_children] || []).each_with_index do |h,i|
            return false unless match_wrapper(h, children[i], name)
          end
          true
        end

        def failure_message_for_should
          require 'pp'
          "expected #{@actual.name} to have structure:\n#{@expected.pretty_inspect}\n#{@message}"
        end

        def failure_message_for_should_not
          require 'pp'
          "expected #{@actual.name} to not have structure:\n#{@expected.pretty_inspect}\n#{@message}"
        end
      end

      def have_structure(hash)
        TaskStructureMatcher.new(hash)
      end

      def have_configured_structure(hash)
        TaskStructureMatcher.new(hash, true)
      end

      def example_ant(options = {}, &block)
        Ant.new({:basedir => Dir::tmpdir, :run => false, :output_level => 0}.merge(options),&block)
      end
    end
  end
end

def hide_ant_from_path
  env=[]
  ENV['PATH'].split(File::PATH_SEPARATOR).each do |dir|
    if ! File.executable?(File.join(dir, 'ant'))
      env << dir
    end
  end
  ENV['PATH'] = env.join(File::PATH_SEPARATOR)
end
