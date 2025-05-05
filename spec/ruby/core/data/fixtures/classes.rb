module DataSpecs
  guard -> { ruby_version_is "3.2" and Data.respond_to?(:define) } do
    Measure = Data.define(:amount, :unit)

    class MeasureWithOverriddenName < Measure
      def self.name
        "A"
      end
    end

    class DataSubclass < Data; end

    MeasureSubclass = Class.new(Measure) do
      def initialize(amount:, unit:)
        super
      end
    end

    Empty = Data.define()
  end
end
