#!/usr/bin/env ruby
#
# The Computer Language Shootout
#   http://shootout.alioth.debian.org
#   contributed by Kevin Barnes (Ruby novice)

def blank_board
  0b111111100000100000100000100000100000100000100000100000100000100000
end

def is_even( location)
  (location % 12) < 6
end

def create_collector_support
    odd_map = [0b11, 0b110, 0b1100, 0b11000, 0b10000] 
    even_map = [0b1, 0b11, 0b110, 0b1100, 0b11000] 
    
    all_odds = Array.new(0b100000)
    all_evens = Array.new(0b100000)
    bit_counts = Array.new(0b100000)
    new_regions = Array.new(0b100000)
    0.upto(0b11111) do | i |
      bit_count = odd = even = 0
      0.upto(4) do | bit |
        if (i[bit] == 1) then
          bit_count += 1
          odd |= odd_map[bit]
          even |= even_map[bit]
        end
      end
      all_odds[i] = odd
      all_evens[i] = even
      bit_counts[i] = bit_count
      new_regions[i] = create_regions( i)
    end

    @@converter = []
    10.times { | row | @@converter.push((row % 2 == 0) ? all_evens : all_odds) }
    @@bit_counts = bit_counts
    @@regions = new_regions.collect { | set | set.collect { | value | [ value, bit_counts[value], value] } }

  end
  
def prunable( board, location, slotting = false)
  collectors = []
  (location / 6).to_i.upto(9) do | row_on | 
    regions = @@regions[(board >> (row_on * 6)) & 0b11111 ^ 0b11111]
    converter = @@converter[row_on]
    initial_collector_count = collectors.length
    regions.each do | region |
      collector_found = nil
      region_mask = region[0]
      initial_collector_count.times do | collector_num |
        collector = collectors[collector_num]
        if (collector) then
          collector_mask = collector[0]
          if (collector_mask & region_mask != 0) then
            if (collector_found) then
              collector_found[0] |= collector_mask
              collector_found[1] += collector[1]
              collector_found[2] |= collector[2]
              collectors[collector_num] = nil
            else
              collector_found = collector
              collector[1] += region[1]
              collector[2] |= region_mask
            end
          end
        end
      end
      if (collector_found == nil) then
        collectors.push(Array.new(region))
      end
    end
    collectors.length.times do | collector_num |
      collector = collectors[collector_num]
      if (collector) then
        if (collector[2] == 0) then
          return true if (collector[1] % 5 != 0)
          collectors[collector_num] = nil
        else
          return false if (collector[2] == 0b11111 && !slotting)
          collector[0] = converter[collector[2]]
          collector[2] = 0
        end
      end
    end
    collectors.compact!
  end
  return false if (collectors.length <= 1) 
  collectors.any? { | collector | (collector[1] % 5) != 0 }
end
  
def as_binary( value)
  rtn = ""
  5.times do | i |
    rtn += "#{value[i]}"
  end
  rtn
end
  
def create_regions( value )
  regions = []
  cur_region = 0
  5.times do | bit |
    if (value[bit] == 1) then
      cur_region |= 1 << bit
    else
      if (cur_region !=0 ) then
        regions.push( cur_region)
        cur_region = 0;
      end
    end
  end
  regions.push(cur_region) if (cur_region != 0)
  regions
end

def print_board( board, padding = "", rows = 10, row_offset = 0)
  rows.times do | row |
    rtn = padding
    rtn = "#{rtn} " if ((row + row_offset) % 2) == 1 
    6.times do | col | 
      rtn = "#{rtn}#{board[row*6+col]} " 
    end
    print "#{rtn}\n"
  end
end  

class Rotation
  attr_reader :start_masks
  
  @@rotation_even_adder = { :west => -1, :east => 1, :nw => -7, :ne => -6, :sw => 5, :se => 6 }
  @@rotation_odd_adder = { :west => -1, :east => 1, :nw => -6, :ne => -5, :sw => 6, :se => 7 }
  
  def initialize( directions )
    values, min = get_values( directions )
    @even_offsets, @odd_offsets = normalize_offsets( values, min)
      
    @even_mask = mask_for_offsets( @even_offsets)
    @odd_mask = mask_for_offsets( @odd_offsets)

    @start_masks = Array.new(60)
    
    0.upto(59) do | offset |
      mask = is_even(offset) ? (@even_mask << offset) : (@odd_mask << offset)
      if (blank_board & mask == 0 && !prunable(blank_board | mask, 0, true)) then
        @start_masks[offset] = mask
      else
        @start_masks[offset] = false 
      end
    end
  end
  
  def offsets( location)
    if is_even( location) then
      @even_offsets.collect { | value | value + location }
    else
      @odd_offsets.collect { | value | value + location }
    end
  end
  
  def normalize_offsets( values, min)
    even_min = is_even(min)
    other_min = even_min ? min + 6 : min + 7
    other_values = values.collect do | value | 
      if is_even(value) then 
        value + 6 - other_min 
      else 
        value + 7 - other_min 
      end
    end
    values.collect! { | value | value - min }
    
    if even_min then
      [values, other_values]
    else
      [other_values, values]
    end
  end
  
  def mask_for_offsets( offsets )
    mask = 0
    offsets.each { | value | mask = mask + ( 1 << value ) }
    mask
  end

  def start_adjust( directions )
    south = east = 0;
    directions.each do | direction |
      east += 1 if ( direction == :sw || direction == :nw || direction == :west )
      south += 1 if ( direction == :nw || direction == :ne )   
    end
    [south, east]
  end

  def get_values ( directions )
    south, east = start_adjust(directions)
    min = start = south * 6 + east
    values = [ start ]
    directions.each do | direction |
      if (start % 12 >= 6) then 
        start += @@rotation_odd_adder[direction]
      else 
        start += @@rotation_even_adder[direction]
      end
      min = start if (start < min)
      values += [ start ]
    end
    
    if (values.length != 5)
      values.uniq!
    end
    
    [ values, min ]
  end
end

class Piece
  attr_reader :rotations, :type, :masks
  attr_accessor :placed
  
  @@flip_converter = { :west => :west, :east => :east, :nw => :sw, :ne => :se, :sw => :nw, :se => :ne }
  @@rotate_converter = { :west => :nw, :east => :se, :nw => :ne, :ne => :east, :sw => :west, :se => :sw }
  
  def initialize( directions, type )
    @type = type
    @rotations = Array.new();
    @map = {}
    generate_rotations( directions )
    directions.collect! { | value | @@flip_converter[value] }
    generate_rotations( directions )
    
    @masks = Array.new();
    0.upto(59) do | i |
      @masks[i] = @rotations.collect do | rotation | 
        mask = rotation.start_masks[i]
        @map[mask] = [ i, rotation ] if (mask) 
        mask || nil
      end
      @masks[i].compact!
    end
  end
  
  def generate_rotations( directions ) 
    6.times do
      rotations.push( Rotation.new(directions))
      directions.collect! { | value | @@rotate_converter[value] }
    end
  end
  
  def fill_array( board_array)
    location, rotation = @map[@placed]
    rotation.offsets(location).each do | offset |
      row, col = offset.divmod(6)
      board_array[ row*5 + col ] = @type.to_s
    end
  end
end

class Processor 
  attr :pieces, :board
  
  def initialize() 
    create_collector_support
    @pieces = [ 
      Piece.new( [ :east, :east, :east, :se ], 0),
      Piece.new( [ :ne, :east, :ne, :nw ], 1),
      Piece.new( [ :nw, :ne, :east, :east ], 2),
      Piece.new( [ :east, :east, :sw, :se ], 3),
      Piece.new( [ :ne, :nw, :se, :east, :se ], 4),
      Piece.new( [ :east, :ne, :se, :ne ], 5),
      Piece.new( [ :east, :sw, :sw, :se ], 6),
      Piece.new( [ :ne, :se, :east, :ne ], 7),
      Piece.new( [ :se, :se, :east, :se ], 8),
      Piece.new( [ :se, :se, :se, :west ], 9) ];
      
    @all_pieces = Array.new( @pieces)

    @min_board = "99999999999999999999999999999999999999999999999999"
    @max_board = "00000000000000000000000000000000000000000000000000"
    @stop_count = ARGV[0].to_i || 2089
    @all_boards = {}
    @boards_found = 0
  end
  
  def find_all
    find_top( 0)
    find_top( 1)
    print_results
  end

  def print_results
    print "#{@boards_found} solutions found\n\n"
    print_full_board( @min_board)
    print "\n"
    print_full_board( @max_board)
    print "\n"
  end

  def find_top( rotation_skip) 
    board = blank_board
    @pieces.length.times do
      piece = @pieces.shift
      piece.masks[0].each do | mask |
        if ((rotation_skip += 1) % 2 == 0) then
          piece.placed = mask
          find( 1, 1, board | mask) 
        end
      end
      @pieces.push(piece)
    end
  end

  def find( start_location, placed, board) 
    while board[start_location] == 1
      start_location += 1 
    end

    return if (start_location < 28 && prunable( board, start_location))
    
    @pieces.length.times do
      piece = @pieces.shift
      piece.masks[start_location].each do | mask |
        if (mask & board == 0) then
          piece.placed = mask
          if (placed == 9) then
            add_board
          else
            find( start_location + 1, placed + 1, board | mask) 
          end
        end
      end
      @pieces.push(piece)
    end
  end
  
  def print_full_board( board_string)
    10.times do | row |
      print " " if (row % 2 == 1) 
      5.times do | col |
        print "#{board_string[row*5 + col,1]} "
      end
      print "\n"
    end
  end
  
  def add_board
    board_array = Array.new(50)
    @all_pieces.each do | piece |
      piece.fill_array( board_array )
    end
    start_board = board_string = board_array.join("")
    save( board_string)
    board_string = flip( board_string)
    save( board_string)
  end

  def flip( board_string)
    new_string = ""
    50.times do | i |
      row, col = i.divmod(5)
      new_string += board_string[((9 - row) * 5) + (4 - col), 1]
    end
    new_string
  end
      
  def save( board_string)
    if (@all_boards[board_string] == nil) then
      @min_board = board_string if (board_string < @min_board)
      @max_board = board_string if (board_string > @max_board)
      @all_boards.store(board_string,true)
      @boards_found += 1

      if (@boards_found == @stop_count) then
        print_results
        exit(0)
      end
    end
  end
  
end

proc = Processor.new.find_all

