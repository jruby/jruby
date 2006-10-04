require 'java'

JFrame = javax.swing.JFrame

frame = JFrame.new("Hello Swing")
frame.getContentPane.add javax.swing.JLabel.new("Hello World")
frame.setDefaultCloseOperation JFrame::EXIT_ON_CLOSE
frame.pack
frame.setVisible true
