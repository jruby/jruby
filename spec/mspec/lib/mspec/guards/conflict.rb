require 'mspec/guards/guard'

class ConflictsGuard < SpecGuard
  def match?
    # Always convert constants to symbols regardless of version.
    constants = Object.constants.map { |x| x.to_sym }
    @args.any? { |mod| constants.include? mod }
  end
end

class Object
  # In some cases, libraries will modify another Ruby method's
  # behavior. The specs for the method's behavior will then fail
  # if that library is loaded. This guard will not run if any of
  # the specified constants exist in Object.constants.
  def conflicts_with(*modules)
    g = ConflictsGuard.new(*modules)
    g.name = :conflicts_with
    yield if g.yield? true
  ensure
    g.unregister
  end
end
