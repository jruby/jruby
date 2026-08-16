# frozen_string_literal: true

# Kestówv 0.5.1 — mm/mm.rb
#
# Top-level Memory Manager.
# Single entry point for the mm subsystem.
# All mm operations should go through here at the process/kernel level.

module Kestowv
  module Mm
    module MemoryManager

      @initialized = false
      @mutex       = Mutex.new

      class << self

        # --------------------------------------------------------
        # BOOT REGISTRATION
        # --------------------------------------------------------

        def register_with_boot
          Boot.register(:mm)
          Boot.set_bit(:mm)
          self
        end

        # --------------------------------------------------------
        # INITIALIZATION
        # --------------------------------------------------------

        # Initialize the mm subsystem with a given frame count.
        # Safe to call once — raises if called again without reset!
        def init(total_frames, page_size: 4096)
          @mutex.synchronize do
            raise "MemoryManager already initialized — call reset! first" if @initialized

            FrameAllocator.init(total_frames, page_size: page_size)

            @total_frames = total_frames
            @page_size    = page_size
            @initialized  = true
          end

          Boot.set_bit(:mm_initialized)
          self
        end

        def initialized?
          @mutex.synchronize { @initialized }
        end

        # Reset for testing or re-initialization.
        def reset!
          @mutex.synchronize do
            @initialized  = false
            @total_frames = 0
          end
          Boot.clear_bit(:mm_initialized)
          self
        end

        # --------------------------------------------------------
        # FACTORY
        # --------------------------------------------------------

        # Create a new virtual address space.
        # Optionally pre-populate with a set of VmRegions.
        def create_vm_space(name: nil, regions: [])
          space = VmSpace.new(name: name)
          regions.each { |r| space.add_region(r) }
          space
        end

        # Create a VmRegion with Protection constants.
        def create_region(start_vpn:, num_pages:, prot: Protection::USER_RW,
                          name: nil, backing: :anonymous)
          VmRegion.new(
            start_vpn: start_vpn,
            num_pages: num_pages,
            flags:     prot,
            name:      name,
            backing:   backing
          )
        end

        # Allocate a physical frame directly (for kernel use).
        def alloc_frame
          guard_initialized!
          FrameAllocator.allocate
        end

        def free_frame(page)
          FrameAllocator.free(page)
        end

        # --------------------------------------------------------
        # GC LOAD-BALANCE SIGNAL
        # Called by any allocator when heap pressure is detected.
        # Propagates to Core::Wave governor so CPU load backs off and
        # gives the JVM GC headroom to run without competition.
        # --------------------------------------------------------

        def signal_gc_pressure(level)
          Pressure.set_level(level)
          Core::Wave.signal_gc_pressure(level) if defined?(Core::Wave) && Core::Wave.running?
          self
        end

        # --------------------------------------------------------
        # SYSTEM-WIDE STATS
        # --------------------------------------------------------

        def stats
          guard_initialized!

          fa    = FrameAllocator.stats
          fault = Fault.stats
          mem   = Kestowv::Hal::Memory.stats rescue {}

          {
            initialized:     @initialized,
            total_frames:    @total_frames,
            page_size:       @page_size,
            frame_allocator: fa,
            fault:           fault,
            hal_memory:      mem,
            utilization:     FrameAllocator.utilization
          }
        end

        def to_s
          return "#<MemoryManager uninitialized>" unless initialized?
          "#<MemoryManager frames=#{@total_frames} " \
          "util=#{(FrameAllocator.utilization * 100).round(1)}%>"
        end

        private

        def guard_initialized!
          raise "MemoryManager not initialized — call init(total_frames) first" unless @initialized
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :mm_memory_manager,
  __FILE__,
  feature:    :mm,
  depends_on: [
    :mm_protection,
    :mm_page,
    :mm_frame_allocator,
    :mm_page_table,
    :mm_vm_region,
    :mm_fault,
    :mm_vm_space
  ]
)
