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

import groovy.transform.ToString
import jline.Terminal
import jline.TerminalFactory
import org.fusesource.jansi.AnsiRenderWriter

/**
 * Console wrapper
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
@ToString(includeNames = true, includeFields = true, includePackage = false)
class Console {
  /** JLine Terminal. */
  private final Terminal terminal

  /** Preferred input reader. */
  private final Reader input

  /** Preferred output writer. */
  private final PrintWriter output

  /** Preferred error output writer. */
  private final PrintWriter error

  /** Raw input stream. */
  private final InputStream inputStream

  /** Raw output stream. */
  private final OutputStream outputStream

  /** Raw error output stream. */
  private final OutputStream errorStream

  /**
   * Construct a new Console.
   */
  public Console(final InputStream inputStream, final OutputStream outputStream, final OutputStream errorStream) {
    assert inputStream != null
    assert outputStream != null
    assert errorStream != null

    this.terminal = TerminalFactory.create()

    this.inputStream = inputStream
    this.outputStream = outputStream
    this.errorStream = errorStream

    this.input = new InputStreamReader(terminal.wrapInIfNeeded(inputStream))
    if (this.terminal.ansiSupported) {
      this.output = new AnsiRenderWriter(terminal.wrapOutIfNeeded(outputStream), true)
      this.error = new AnsiRenderWriter(terminal.wrapOutIfNeeded(errorStream), true)
    } else {
      this.output = new PrintWriter(terminal.wrapOutIfNeeded(outputStream), true)
      this.error = new PrintWriter(terminal.wrapOutIfNeeded(errorStream), true)
    }
  }

  /**
   * Construct a new Console using system streams.
   */
  public Console() {
    this(System.in, System.out, System.err);
  }

  Reader getIn() {
    input
  }

  PrintWriter getOut() {
    output
  }

  PrintWriter getErr() {
    error
  }

  boolean isSupported() {
    terminal.supported
  }

  int getWidth() { terminal.width }

  int getHeight() { terminal.height }

  boolean isAnsiSupported() { terminal.ansiSupported }

  boolean isEchoEnabled() { terminal.echoEnabled }

  String getOutputEncoding() { terminal.outputEncoding }

}