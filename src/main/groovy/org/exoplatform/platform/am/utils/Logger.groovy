/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This file is part of eXo Platform - Add-ons Manager.
 *
 * eXo Platform - Add-ons Manager is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * eXo Platform - Add-ons Manager software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with eXo Platform - Add-ons Manager; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.platform.am.utils

import org.exoplatform.platform.am.AddonsManagerConstants
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiRenderer
import org.fusesource.jansi.AnsiString

/**
 * A really simple logger
 */
class Logger {

  static final Console console = new Console()

  /**
   * The enumeration of all possible log levels
   */
  private static enum Level {
    DEBUG(Ansi.Color.CYAN, ". "),
    INFO(Ansi.Color.DEFAULT, ""),
    WARN(Ansi.Color.YELLOW, "! "),
    ERROR(Ansi.Color.RED, "- ")
    /**
     * Foreground color used for ascii supported console
     */
    final Ansi.Color color
    /**
     * Text prefix used for non-ascii supported console
     */
    final String prefix

    Level(Ansi.Color color, String prefix) {
      this.color = color
      this.prefix = prefix
    }
  }

  private static boolean debug

  static boolean isDebugEnabled() {
    return debug
  }

  static enableDebug() {
    debug = true
    debug("Verbose logs activated")
  }

  static void debug(final Object msg) {
    if (isDebugEnabled()) {
      log(Level.DEBUG, msg, null);
    }
  }

  static void debug(final Object msg, final Throwable cause) {
    if (isDebugEnabled()) {
      log(Level.DEBUG, msg, cause);
    }
  }

  static void info(final Object msg) {
    log(Level.INFO, msg, null);
  }

  static void info(final Object msg, final Throwable cause) {
    log(Level.INFO, msg, cause);
  }

  static void warn(final Object msg) {
    log(Level.WARN, msg, null);
  }

  static void warn(final Object msg, final Throwable cause) {
    log(Level.WARN, msg, cause);
  }

  static void error(final Object msg) {
    log(Level.ERROR, msg, null);
  }

  static void error(final Object msg, final Throwable cause) {
    log(Level.ERROR, msg, cause);
  }

  static displayHeader(String managerVersion) {
    info("""
    @|yellow               xx      xx |@
    @|yellow                xx    xx  |@
    @|yellow    eeeeeee      xx  xx  |@    ooooooo
    @|yellow  ee       ee     xxxx   |@  oo       @|yellow oo  |@
    @|yellow eeeeeeeeeeeee    xxxx   |@ oo        @|yellow  oo |@
    @|yellow ee              xx  xx  |@ oo        @|yellow  oo |@
    @|yellow  ee       ee   xx    xx |@  oo       @|yellow oo  |@
    @|yellow    eeeeeee    xx      xx    ooooooo |@           Add-ons Manager v @|yellow ${managerVersion} |@
    """)
  }

  static logWithStatus(String text, Closure closure, Object... args) {
    if (console.ansiSupported) {
      console.out.print text
    } else {
      console.out.print new AnsiString(AnsiRenderer.render(text)).plain
    }
    try {
      def result = closure.call(args)
      displayStatus(text, AddonsManagerConstants.STATUS_OK, Ansi.Color.GREEN)
      return result
    } catch (Throwable t) {
      displayStatus(text, AddonsManagerConstants.STATUS_KO, Ansi.Color.RED)
      throw t
    }
  }

  static logWithStatusOK(String text) {
    if (console.ansiSupported) {
      console.out.print text
    } else {
      console.out.print new AnsiString(AnsiRenderer.render(text)).plain
    }
    displayStatus(text, AddonsManagerConstants.STATUS_OK, Ansi.Color.GREEN)
  }

  static logWithStatusKO(String text) {
    if (console.ansiSupported) {
      console.out.print text
    } else {
      console.out.print new AnsiString(AnsiRenderer.render(text)).plain
    }
    displayStatus(text, AddonsManagerConstants.STATUS_KO, Ansi.Color.RED)
  }

  private static void log(final Level level, Object msg, Throwable cause) {
    assert level != null
    assert msg != null

    // Allow the msg to be a Throwable, and handle it properly if no cause is given
    if (cause == null) {
      if (msg instanceof Throwable) {
        cause = (Throwable) msg
        msg = "${cause.getClass()} : ${cause.getMessage()}"
      }
    }

    if (console.ansiSupported) {
      if (Ansi.Color.DEFAULT != level.color) {
        console.out.println "@|${level.color.name()} ${msg}|@"
      } else {
        console.out.println msg
      }
    } else {
      console.out.println "${level.prefix}${new AnsiString(AnsiRenderer.render(msg)).plain}"
    }

    if (cause != null && isDebugEnabled()) {
      cause.printStackTrace(console.out);
    }
  }

  private static displayStatus(String text, String status, Ansi.Color color) {
    String statusStr = " [@|${color.name()} ${status.toUpperCase()}|@]"
    String padding = " ".padRight(console.width - new AnsiString(AnsiRenderer.render("${text}${statusStr}")).length(), ".")
    if (console.ansiSupported) {
      console.out.println "${padding}${statusStr}"
    } else {
      console.out.println new AnsiString(AnsiRenderer.render("${padding}${statusStr}")).plain
    }

  }

}