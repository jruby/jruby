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
      if project.targets.include?(name)
        project.log "ant already defines #{name}.  Redefining as #{ALREADY_DEFINED_PREFIX}#{name}"
        name = ALREADY_DEFINED_PREFIX + name
      end
      name
    end
  end
end