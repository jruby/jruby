require 'mspec/guards/guard'

class BlockDeviceGuard < SpecGuard
  def match?
    platform_is_not :freebsd, :windows, :opal do
      block = `find /dev /devices -type b 2> /dev/null`
      return !(block.nil? || block.empty?)
    end

    false
  end
end

class Object
  def with_block_device
    g = BlockDeviceGuard.new
    g.name = :with_block_device
    yield if g.yield?
  ensure
    g.unregister
  end
end
