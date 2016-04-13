module CoverageSpecs
  # Clear old results from the result hash
  # https://bugs.ruby-lang.org/issues/12220
  def self.filtered_result
    Coverage.result.select { |_k, v| v.any? }
  end
end
