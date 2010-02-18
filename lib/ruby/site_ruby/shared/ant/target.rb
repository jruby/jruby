require 'java'
require 'ant/ant'

java_import org.apache.tools.ant.Target

class Ant
  class RakeTarget < Target
    ALREADY_DEFINED_PREFIX = "rake_"

    def initialize(ant, rake_task)
      super()
      set_project ant.project
      set_name generate_unique_target_name rake_task.name

      rake_task.prerequisites.each { |prereq| add_dependency prereq }

      @rake_task = rake_task
    end

    def execute
      @rake_task.execute
    end

    private
    def generate_unique_target_name(name)
      # FIXME: This is not guaranteed to be unique and may be a wonky naming convention?
      if project.targets.get(name)
        project.log "ant already defines #{name}.  Redefining as #{ALREADY_DEFINED_PREFIX}#{name}"
        name = ALREADY_DEFINED_PREFIX + name
      end
      name
    end
  end

  class BlockTarget < Target
    def initialize(ant, *options, &block)
      super()
      set_project ant.project
      hash = extract_options(options)
      hash.each_pair {|k,v| send("set_#{k}", v) }
      define_target(ant, &block) if block
    end

    private
    def extract_options(options)
      hash = Hash === options.last ? options.pop : {}
      hash[:name] = options[0].to_s if options[0]
      hash[:description] = options[1].to_s if options[1]
      hash
    end

    def define_target(ant, &block)
      ant.current_target = self
      ant.instance_eval(&block)
    ensure
      ant.current_target = nil
    end
  end

  class TargetWrapper
    def initialize(project, name)
      @project, @name = project, name
    end

    def execute
      @project.execute_target(@name)
    end
  end

  class MissingWrapper
    def initialize(project, name)
      @project_name = project.name || "<anonymous>"
      @name = name
    end

    def execute
      raise "Target `#{@name}' does not exist in project `#{@project_name}'"
    end
  end
end
