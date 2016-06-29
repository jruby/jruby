module MSpec
  def self.deprecate(what, replacement)
    $stderr.puts "\n#{what} is deprecated, use #{replacement} instead."
    user_caller = caller.find { |line| !line.include?('lib/mspec') }
    $stderr.puts "from #{user_caller}"
  end
end
