require 'mspec/guards/guard'

class UnspecifiedGuard < SpecGuard
  def match?
    not standard?
  end
end

class SpecifiedOnGuard < SpecGuard
  def match?
    if @args.include? :ruby
      raise Exception, "improper use of specified_on guard"
    end
    not standard? and implementation?(*@args)
  end
end

class Object
  # This guard wraps one or more #specified_on guards to group them and
  # document the specs. The purpose of the guard is for situations where MRI
  # either does not specify Ruby behavior or where MRI's behavior is all but
  # impossible to spec, for example due to relying on platform-specific
  # behavior that is not easily testable from Ruby code. In such cases, it
  # may be desirable for implementations to explore a specified set of
  # behaviors that are explicitly documented in the specs.
  #
  #   unspecified do
  #     specified_on :rubinius, :ironruby do
  #       it "returns true when passed :foo" do
  #         # ...
  #       end
  #
  #       it "returns false when passed :bar" do
  #         # ...
  #       end
  #     end
  #
  #     specified_on :jruby do
  #       it "returns true when passed :bar" do
  #         # ...
  #       end
  #     end
  #   end
  #
  # Note that these guards do not change the policy of the #compliant_on,
  # #not_compliant_on, #deviates_on, #extended_on, and #not_supported_on
  # guards.
  #
  def unspecified
    g = UnspecifiedGuard.new
    g.name = :unspecified
    yield if g.yield?
  ensure
    g.unregister
  end

  # This guard wraps specs for one or more particular implementations. See the
  # #unspecified guard for further documentation.
  def specified_on(*args)
    g = SpecifiedOnGuard.new(*args)
    g.name = :specified_on
    yield if g.yield?
  ensure
    g.unregister
  end
end
