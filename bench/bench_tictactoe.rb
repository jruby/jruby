#!/usr/bin/env ruby

# This is a pretty naive implementation of Tic-Tac-Toe meant to
# demonstrate the minimax algorithm. Note that it takes awhile to
# start up - 30 seconds on a 2010 mac book air. Also note that the
# Ruby isn't all that great as Ruby, but I don't care as long as it
# illustrates the algorithm well.
#
# You can see that there's a hard-coded assumption that the AI is the
# X player, means that no human will ever be able to win. I'm positive
# that is only a small taste of what is to come, once robots take
# over the world.
require 'benchmark'

class GameState
  attr_accessor :current_player, :board, :moves, :rank

  class Cache
    attr_accessor :states
    def initialize
      @states = {}
    end
  end

  class << self
    attr_accessor :cache
  end
  self.cache = Cache.new

  def initialize(current_player, board)
    self.current_player = current_player
    self.board = board
    self.moves = []
  end
  
  def rank
    @rank ||= final_state_rank || intermediate_state_rank
  end

  # this is only ever called when it's the AI's (the X player) turn
  def next_move
    moves.max{ |a, b| a.rank <=> b.rank }
  end

  def final_state_rank
    if final_state?
      return 0 if draw?
      winner == "X" ? 1 : -1
    end
  end

  def final_state?
    winner || draw?
  end

  def draw?
    board.compact.size == 9 && winner.nil?
  end

  def intermediate_state_rank
    # recursion, baby
    ranks = moves.collect{ |game_state| game_state.rank }
    if current_player == 'X'
      ranks.max
    else
      ranks.min
    end
  end  

  def winner
    @winner ||= [
     # horizontal wins
     [0, 1, 2],
     [3, 4, 5],
     [6, 7, 8],

     # vertical wins
     [0, 3, 6],
     [1, 4, 7],
     [2, 5, 8],

     # diagonal wins
     [0, 4, 8],
     [6, 4, 2]
    ].collect { |positions|
      ( board[positions[0]] == board[positions[1]] &&
        board[positions[1]] == board[positions[2]] &&
        board[positions[0]] ) || nil
    }.compact.first
  end
end

class GameTree
  def generate
    initial_game_state = GameState.new('X', Array.new(9))
    generate_moves(initial_game_state)
    initial_game_state
  end

  def generate_moves(game_state)
    next_player = (game_state.current_player == 'X' ? 'O' : 'X')
    game_state.board.each_with_index do |player_at_position, position|
      unless player_at_position
        next_board = game_state.board.dup
        next_board[position] = game_state.current_player

        next_game_state = (GameState.cache.states[next_board] ||= GameState.new(next_player, next_board))
        game_state.moves << next_game_state
        generate_moves(next_game_state)
      end
    end
  end
end

class Game
  def initialize
    puts Benchmark.measure{ @game_state = @initial_game_state = GameTree.new.generate }
  end

  def turn
    if @game_state.final_state?
      describe_final_game_state
      puts "Play again? y/n"
      answer = gets
      if answer.downcase.strip == 'y'
        @game_state = @initial_game_state
        turn
      else
        exit
      end
    end
    
    if @game_state.current_player == 'X'
      puts "\n==============="
      @game_state = @game_state.next_move
      puts "X's move:"
      render_board
      turn
    else
      get_human_move
      puts "The result of your move:"
      render_board
      puts ""
      turn
    end
  end
  
  def render_board
    output = ""
    0.upto(8) do |position|
      output << " #{@game_state.board[position] || position} "
      case position % 3
      when 0, 1 then output << "|"
      when 2 then output << "\n-----------\n" unless position == 8
      end
    end
    puts output
  end

  def get_human_move
    puts "Enter square # to place your 'O' in:"
    position = gets

    move = @game_state.moves.find{ |game_state| game_state.board[position.to_i] == 'O' }

    if move
      @game_state = move
    else
      puts "That's not a valid move"
      get_human_move
    end
  end

  def describe_final_game_state
    if @game_state.draw?
      puts "It was a draw!"
    elsif @game_state.winner == 'X'
      puts "X won!"
    else
      puts "O won!"
    end
  end
end

#Game.new.turn
10.times { puts Benchmark.measure { GameTree.new.generate } }
