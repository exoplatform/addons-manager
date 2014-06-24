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

  /**
   * Singleton
   */
  private static final Logger singleton = new Logger()

  /**
   * Factory
   * @return The {@link Logger} singleton instance
   */
  static Logger getInstance() {
    return singleton
  }

  private final Console console

  /**
   * The enumeration of all possible log levels
   */
  private enum Level {
    DEBUG(Ansi.Color.CYAN, "[DEBUG] "),
    INFO(Ansi.Color.DEFAULT, ""),
    WARN(Ansi.Color.MAGENTA, "[WARN]  "),
    ERROR(Ansi.Color.RED, "[ERROR] ")
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

  Logger() {
    this.console = Console.get()
  }

  Logger(Console console) {
    this.console = console
  }

  private boolean _debug

  boolean isDebugEnabled() {
    return _debug
  }

  void enableDebug() {
    _debug = true
    debug("Verbose logs activated")
  }

  void debug(final Object msg) {
    if (isDebugEnabled()) {
      log(Level.DEBUG, msg, null);
    }
  }

  void debug(final Object msg, final Throwable cause) {
    if (isDebugEnabled()) {
      log(Level.DEBUG, msg, cause);
    }
  }

  void debug(final String title, final Map map, final List<String> excludes) {
    List<String> fieldsToExcludes = excludes ? excludes : []
    debug("".padRight(Console.get().width - Level.DEBUG.prefix.length(), "="))
    debug("${title.toUpperCase()}:")
    debug("".padRight(Console.get().width - Level.DEBUG.prefix.length(), "="))
    if (map) {
      map.keySet().findAll { !fieldsToExcludes.contains(it) }.each {
        debug String.format("%-${map.keySet()*.size().max()}s : %s", it, map.get(it))
      }
    } else {
      debug "Null"
    }
  }

  void info(final Object msg) {
    log(Level.INFO, msg, null);
  }

  void info(final Object msg, final Throwable cause) {
    log(Level.INFO, msg, cause);
  }

  void warn(final Object msg) {
    log(Level.WARN, msg, null);
  }

  void warn(final Object msg, final Throwable cause) {
    log(Level.WARN, msg, cause);
  }

  void error(final Object msg) {
    log(Level.ERROR, msg, null);
  }

  void error(final Object msg, final Throwable cause) {
    log(Level.ERROR, msg, cause);
  }

  void displayHeader(String managerVersion) {
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

  def withStatus(String text, Closure closure, Object... args) {
    if (console.ansiSupported) {
      console.out.print text
    } else {
      console.out.print new AnsiString(AnsiRenderer.render(text)).plain
    }
    console.out.flush()
    try {
      def result = closure.call(args)
      displayStatus(text, AddonsManagerConstants.STATUS_OK, Ansi.Color.GREEN)
      return result
    } catch (Throwable t) {
      displayStatus(text, AddonsManagerConstants.STATUS_KO, Ansi.Color.RED)
      throw t
    }
  }

  void withStatusOK(String text) {
    if (console.ansiSupported) {
      console.out.print text
    } else {
      console.out.print new AnsiString(AnsiRenderer.render(text)).plain
    }
    displayStatus(text, AddonsManagerConstants.STATUS_OK, Ansi.Color.GREEN)
  }

  void withStatusKO(String text) {
    if (console.ansiSupported) {
      console.out.print text
    } else {
      console.out.print new AnsiString(AnsiRenderer.render(text)).plain
    }
    displayStatus(text, AddonsManagerConstants.STATUS_KO, Ansi.Color.RED)
  }

  private void log(final Level level, Object msg, Throwable cause) {
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
        console.out.println "@|${level.color.name()} ${level.prefix}${new AnsiString(AnsiRenderer.render(msg)).plain}|@"
      } else {
        console.out.println "${level.prefix}${AnsiRenderer.render(msg)}"
      }
    } else {
      console.out.println "${level.prefix}${new AnsiString(AnsiRenderer.render(msg)).plain}"
    }

    if (cause != null && isDebugEnabled()) {
      cause.printStackTrace(console.out);
    }
  }

  private void displayStatus(String text, String status, Ansi.Color color) {
    String statusStr = " [@|${color.name()} ${status.toUpperCase()}|@]"
    String padding = " ".padRight(console.width - new AnsiString(AnsiRenderer.render("${text}${statusStr}")).length(), ".")
    if (console.ansiSupported) {
      console.out.println "${padding}${statusStr}"
    } else {
      console.out.println new AnsiString(AnsiRenderer.render("${padding}${statusStr}")).plain
    }

  }

}