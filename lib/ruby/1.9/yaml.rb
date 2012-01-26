module YAML
  class EngineManager # :nodoc:
    attr_reader :yamler

    def initialize
      @yamler = nil
    end

    def syck?
      'syck' == @yamler
    end

    def yamler= engine
      raise(ArgumentError, "bad engine") unless %w{syck psych}.include?(engine)

      # JRuby only supports psych, so we warn and ignore
      if engine != 'psych'
        warn "JRuby 1.9 mode only supports the `psych` YAML engine; ignoring `#{engine}`"
        return
      end

      require engine

      Object.class_eval <<-eorb, __FILE__, __LINE__ + 1
        remove_const 'YAML'
        YAML = #{engine.capitalize}
        remove_method :to_yaml
        alias :to_yaml :#{engine}_to_yaml
      eorb

      @yamler = engine
      engine
    end
  end

  ENGINE = YAML::EngineManager.new
end

# JRuby defaults to Psych, to avoid having to use Yecht/Syck in 1.9 mode
engine = (defined?(Syck) && !defined?(Psych) ? 'syck' : 'psych')

module Syck
  ENGINE = YAML::ENGINE
end

module Psych
  ENGINE = YAML::ENGINE
end

YAML::ENGINE.yamler = engine
