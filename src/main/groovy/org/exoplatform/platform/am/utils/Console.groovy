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

import jline.TerminalFactory
import jline.console.ConsoleReader
import org.fusesource.jansi.AnsiRenderWriter

/**
 * Console wrapper
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class Console {

  private final int MAX_CONSOLE_WIDTH_TO_USE = 120

  /** Preferred output writer. */
  private PrintWriter _out

  /** Preferred error output writer. */
  private PrintWriter _err

  /** JLine Console Reader */
  private ConsoleReader _console

  /** Raw input stream. */
  private final InputStream _inputStream

  /** Raw output stream. */
  private final OutputStream _outputStream

  /** Raw error output stream. */
  private final OutputStream _errorStream

  /**
   * Construct a new Console.
   */
  public Console(final InputStream inputStream, final OutputStream outputStream, final OutputStream errorStream) {
    assert inputStream != null
    assert outputStream != null
    assert errorStream != null

    this._inputStream = inputStream
    this._outputStream = outputStream
    this._errorStream = errorStream

    reset()
  }

  /**
   * Construct a new Console using system streams.
   */
  public Console() {
    this(System.in, System.out, System.err);
  }

  PrintWriter getOut() {
    this._out
  }

  PrintWriter getErr() {
    this._err
  }

  boolean isSupported() {
    TerminalFactory.get().supported
  }

  int getWidth() { Math.min(TerminalFactory.get().width, MAX_CONSOLE_WIDTH_TO_USE) }

  int getHeight() { TerminalFactory.get().height }

  boolean isAnsiSupported() { TerminalFactory.get().ansiSupported }

  boolean isEchoEnabled() { TerminalFactory.get().echoEnabled }

  String getOutputEncoding() { TerminalFactory.get().outputEncoding }

  void reset() {
    TerminalFactory.get().restore()
    // Close current out/err
    this._out?.close()
    this._err?.close()
    this._console?.shutdown()
    // Create new out/err (Terminal configuration may have changed - used in tests)
    this._console = new ConsoleReader(this._inputStream, this._outputStream)
    if (TerminalFactory.get().ansiSupported) {
      this._out = new AnsiRenderWriter(TerminalFactory.get().wrapOutIfNeeded(this._outputStream), true)
      this._err = new AnsiRenderWriter(TerminalFactory.get().wrapOutIfNeeded(this._errorStream), true)
    } else {
      this._out = new PrintWriter(TerminalFactory.get().wrapOutIfNeeded(this._outputStream), true)
      this._err = new PrintWriter(TerminalFactory.get().wrapOutIfNeeded(this._errorStream), true)
    }
  }

  String readLine() {
    _console.readLine()
  }

  int read() {
    _console.readCharacter()
  }

// Factory
  private static Console instance = new Console()

  static Console get() {
    Console.instance
  }


}