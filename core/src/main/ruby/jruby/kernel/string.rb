class String
  def unpack1(spec)
    catch(:unpack1) do
      unpack(spec) do |value|
        throw(:unpack1, value)
      end
    end
  end
end