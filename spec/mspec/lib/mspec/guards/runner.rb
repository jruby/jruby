require 'mspec/guards/guard'

class RunnerGuard < SpecGuard
  def match?
    @args.any? do |runner|
      case runner
      when :mspec
        ENV['MSPEC_RUNNER'] == '1'
      when :rspec
        ENV['RSPEC_RUNNER'] == '1' or Object.const_defined?(:Spec)
      else
        false
      end
    end
  end
end

class Object
  def runner_is(*args)
    g = RunnerGuard.new(*args)
    g.name = :runner_is
    yield if g.yield?
  ensure
    g.unregister
  end

  def runner_is_not(*args)
    g = RunnerGuard.new(*args)
    g.name = :runner_is_not
    yield if g.yield? true
  ensure
    g.unregister
  end
end
