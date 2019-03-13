class String
  def unpack1(spec)
    unpack(spec) do |value|
      return value
    end
  end
end