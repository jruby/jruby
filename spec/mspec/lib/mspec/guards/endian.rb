require 'mspec/guards/guard'

# Despite that these are inverses, the two classes are
# used to simplify MSpec guard reporting modes

class EndianGuard < SpecGuard
  def pattern
    @pattern ||= [1].pack('L')
  end
  private :pattern
end

class BigEndianGuard < EndianGuard
  def match?
    pattern[-1] == ?\001
  end
end

class LittleEndianGuard < EndianGuard
  def match?
    pattern[-1] == ?\000
  end
end

class Object
  def big_endian
    g = BigEndianGuard.new
    g.name = :big_endian
    yield if g.yield?
  ensure
    g.unregister
  end

  def little_endian
    g = LittleEndianGuard.new
    g.name = :little_endian
    yield if g.yield?
  ensure
    g.unregister
  end
end
