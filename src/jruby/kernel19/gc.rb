module GC
  def self.stat
    require 'java'
    gc_beans = java.lang.management.ManagementFactory.garbage_collector_mx_beans
    pool_beans = {}
    java.lang.management.ManagementFactory.memory_pool_mx_beans.each do |pool_bean|
      pool_beans[pool_bean.name] = pool_bean
    end

    all_stats = {}

    gc_beans.each do |gc_bean|
      gc_stats = all_stats[gc_bean.name] = {}
      gc_stats[:count] = gc_bean.collection_count
      gc_stats[:time] = gc_bean.collection_time

      gc_bean.memory_pool_names.each do |pool_name|
        pool_bean = pool_beans[pool_name]

        all_pools = gc_stats[:pools] = {}
        pool_stats = all_pools[pool_name] = {}

        usage = pool_bean.usage
        peak_usage = pool_bean.peak_usage
        last_usage = pool_bean.collection_usage

        pool_stats[:committed] = usage.committed
        pool_stats[:init] = usage.init
        pool_stats[:max] = usage.max
        pool_stats[:used] = usage.used
        pool_stats[:peak_committed] = peak_usage.committed
        pool_stats[:peak_init] = peak_usage.init
        pool_stats[:peak_max] = peak_usage.max
        pool_stats[:peak_used] = peak_usage.used
        pool_stats[:last_committed] = last_usage.committed
        pool_stats[:last_init] = last_usage.init
        pool_stats[:last_max] = last_usage.max
        pool_stats[:last_used] = last_usage.used
      end
    end

    all_stats
  end

  module Profiler
    def self.enabled?
      @gc_beans != nil
    end

    def self.enable
      require 'java'
      @gc_beans ||= java.lang.management.ManagementFactory.garbage_collector_mx_beans
      clear
    end

    def self.disable
      @gc_beans = nil
    end

    def self.clear
      return unless @gc_beans

      time = 0
      @gc_beans.each do |gc_bean|
        time += gc_bean.collection_time
      end
      @start_time = time
    end

    def self.result
      nil
    end

    def self.report
      nil
    end

    def self.total_time
      time = 0
      @gc_beans.each do |gc_bean|
        time += gc_bean.collection_time
      end
      (time - @start_time) / 1000.0
    end
  end
end