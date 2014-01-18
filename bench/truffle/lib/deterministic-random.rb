class DeterministicRandom
  FIXNUM_MAX = 0x3fffffff
  FIXNUM_MAX_AS_FLOAT = FIXNUM_MAX.to_f

  def initialize
    @seed = 49734321
  end

  def rand
    @seed = ((@seed + 0x7ed55d16) + (@seed << 12))  & FIXNUM_MAX
    @seed = ((@seed ^ 0xc761c23c) ^ (@seed >> 19))  & FIXNUM_MAX
    @seed = ((@seed + 0x165667b1) + (@seed << 5))   & FIXNUM_MAX
    @seed = ((@seed + 0xd3a2646c) ^ (@seed << 9))   & FIXNUM_MAX
    @seed = ((@seed + 0xfd7046c5) + (@seed << 3))   & FIXNUM_MAX
    @seed = ((@seed ^ 0xb55a4f09) ^ (@seed >> 16))  & FIXNUM_MAX
    @seed / FIXNUM_MAX_AS_FLOAT
  end
end
