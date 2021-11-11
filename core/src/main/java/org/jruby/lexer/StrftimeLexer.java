/*
 * Copyright (c) 2017, 2020 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.jruby.lexer;

import org.jruby.util.ByteList;
import org.jruby.util.RubyDateFormatter.Token;
import org.jruby.util.RubyTimeOutputFormatter;

import static org.jruby.lexer.LexingCommon.PERCENT;

public class StrftimeLexer {
  public char EOF = 0;

  private ByteList input;
  private int length;
  private int n = 0;

  public void reset(ByteList input) {
    this.input = input;
    length = input.length();
    n = 0;
  }

  public Token directive(char c) {
    Token token;
    if ((token = Token.format(c)) != null) {
      return token;
    } else {
      return Token.special(c);
    }
  }

  public Token formatter(ByteList flags, Integer widthString) {
    int width = 0;
    if (widthString != null) {
      width = widthString;
    }

    return Token.formatter(new RubyTimeOutputFormatter(flags, width));
  }

  private char current() {
    return n >= length ? EOF : input.charAt(n); // FIXME: m17n broken
  }

  private char peek() {
    return n + 1 >= length ? EOF : input.charAt(n + 1); // FIXME: m17n broken.
  }

  private boolean consume(char c) {
    if (current() == c) {
      n++;
      return true;
    } else {
      return false;
    }
  }

  private Token next = null;

  public Token yylex() {
      final Token nextToken = next;
      if (nextToken != null) {
        next = null;
        return nextToken;
      }

      if (n >= length) {
        return null;
      }

      if (consume('%')) {
        Token token;
        if ((token = parseLiteralPercent()) != null) {
          return token;
        } else if ((token = parseSimpleDirective()) != null) {
          return token;
        } else if ((token = parseComplexDirective()) != null) {
          return token;
        } else {
          // Invalid modifier/directive, interpret literally
          return Token.str(PERCENT);
        }
      } else {
        return parseUnknown();
      }
  }

  private Token parseLiteralPercent() {
    if (consume('%')) {
      return Token.str(PERCENT);
    }
    return null;
  }

  private Token parseSimpleDirective() {
    return parseConversion();
  }

  private Token parseComplexDirective() {
    int from = n;
    ByteList flags;
    Integer width;
    Token directive;

    if ((flags = parseFlags()) != null) {
      width = parseWidth();
      if ((directive = parseConversion()) != null) {
        next = directive;
        return formatter(flags, width);
      }
    } else if ((width = parseWidth()) != null) {
      if ((directive = parseConversion()) != null) {
        next = directive;
        return formatter(ByteList.EMPTY_BYTELIST, width);
      }
    }

    n = from;
    return null;

  }

  private ByteList parseFlags() {
    int from = n;
    if (n < length && parseFlag()) {
      n++;
      while (n < length && parseFlag()) {
        n++;
      }
      return input.makeShared(from, n - from);
    } else {
      return null;
    }
  }

  private boolean parseFlag() {
    switch (current()) {
      case '-':
      case '_':
      case '0':
      case '#':
      case '^':
        return true;

      default:
        return false;
    }
  }

  private Integer parseWidth() {
    int character = current();
    if (character < '1'  || character > '9') return null;
    n++;
    int count = (character - '0');
    for (character = current(); character != EOF; character = current()) {
      n++;
      if (!isDecimalDigit(character)) {
        n--;
        break;
      }
      count = count * 10 + (character - '0');
    }
    return count;
  }

    private Token parseUnknown() {
      final int from = n;
      while (n < length && current() != '%') {
        n++;
      }
      return Token.str(input.makeShared(from, n - from));
    }

    private Token parseConversion() {
      char c = current();

      // Directive [+AaBbCcDdeFGgHhIjkLlMmNnPpQRrSsTtUuVvWwXxYyZ]
      switch (c) {
        case '+':
        case 'A':
        case 'a':
        case 'B':
        case 'b':
        case 'C':
        case 'c':
        case 'D':
        case 'd':
        case 'e':
        case 'F':
        case 'G':
        case 'g':
        case 'H':
        case 'h':
        case 'I':
        case 'j':
        case 'k':
        case 'L':
        case 'l':
        case 'M':
        case 'm':
        case 'N':
        case 'n':
        case 'P':
        case 'p':
        case 'Q':
        case 'R':
        case 'r':
        case 'S':
        case 's':
        case 'T':
        case 't':
        case 'U':
        case 'u':
        case 'V':
        case 'v':
        case 'W':
        case 'w':
        case 'X':
        case 'x':
        case 'Y':
        case 'y':
        case 'Z':
          n++;
          return directive(c);

        // Ignored modifiers, from MRI strftime.c
        case 'E':
          final char afterE = peek();
          switch (afterE) {
            case 'C':
            case 'c':
            case 'X':
            case 'x':
            case 'Y':
            case 'y':
              n += 2;
              return directive(afterE);
            default:
              return null;
          }
        case 'O':
          final char afterO = peek();
          switch (afterO) {
            case 'd':
            case 'e':
            case 'H':
            case 'k':
            case 'I':
            case 'l':
            case 'M':
            case 'm':
            case 'S':
            case 'U':
            case 'u':
            case 'V':
            case 'W':
            case 'w':
            case 'y':
              n += 2;
              return directive(afterO);
            default:
              return null;
          }

          // Zone
        case 'z':
          n++;
          return Token.zoneOffsetColons(0);
        case ':':
          int from = n;
          n++;
          if (consume(':')) {
            if (consume(':')) {
              if (consume('z')) {
                return Token.zoneOffsetColons(3);
              } else {
                n = from;
                return null;
              }
            } else if (consume('z')) {
              return Token.zoneOffsetColons(2);
            } else {
              n = from;
              return null;
            }
          } else if (consume('z')) {
            return Token.zoneOffsetColons(1);
          } else {
            n = from;
            return null;
          }

        default:
          return null;
      }
    }

    private static boolean isDecimalDigit(int character) {
      return character >= '0' && character <= '9';
    }
  }
