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
import org.exoplatform.platform.am.UnitTestsSpecification
import org.fusesource.jansi.AnsiRenderer
import spock.lang.Shared
import spock.lang.Subject

/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class LoggerTest extends UnitTestsSpecification {

  @Shared
  ByteArrayOutputStream outbaos
  @Shared
  ByteArrayOutputStream errbaos
  @Shared
  ByteArrayInputStream inbais
  @Shared
  Console console
  @Shared
  @Subject
  Logger logger

  def setupSpec() {
    outbaos = new ByteArrayOutputStream()
    errbaos = new ByteArrayOutputStream()
    inbais = new ByteArrayInputStream()
    console = new Console(inbais, outbaos, errbaos)
    logger = new Logger(console)
  }

  def setup() {
    outbaos.reset()
    errbaos.reset()
    inbais.reset()
  }

  def cleanup() {
    System.setProperty(TerminalFactory.JLINE_TERMINAL, TerminalFactory.AUTO)
  }

  def "Debug not activated"() {
    setup:
    console.reset()
    when:
    logger.debug("This @|blue is|@ a test")
    then:
    assert "".contentEquals(outbaos.toString())
  }

  def "Debug activated with ANSI"() {
    setup:
    // Let's force to have an ANSI enable console
    System.setProperty(TerminalFactory.JLINE_TERMINAL, TerminalFactory.UNIX)
    console.reset()
    // Let's activate debug logs
    logger.enableDebug()
    // Let's reset out to be sure to have nothing in it
    outbaos.reset()
    when:
    logger.debug("This @|blue is|@ a test")
    then:
    assert AnsiRenderer.render("@|${Logger.Level.DEBUG.color} ${Logger.Level.DEBUG.prefix}This is a test|@\n").contentEquals(
        outbaos.toString())
    cleanup:
    // Let's deactivate debug logs
    logger.disableDebug()
  }

  def "Debug activated without ANSI"() {
    setup:
    // Let's force to not have an ANSI enable console
    System.setProperty(TerminalFactory.JLINE_TERMINAL, TerminalFactory.OFF)
    console.reset()
    // Let's activate debug logs
    logger.enableDebug()
    // Let's reset out to be sure to have nothing in it
    outbaos.reset()
    when:
    logger.debug("This @|blue is|@ a test")
    then:
    assert "${Logger.Level.DEBUG.prefix}This is a test\n".contentEquals(outbaos.toString())
    cleanup:
    // Let's deactivate debug logs
    logger.disableDebug()
  }

  def "Info with ANSI"() {
    setup:
    // Let's force to have an ANSI enable console
    System.setProperty(TerminalFactory.JLINE_TERMINAL, TerminalFactory.UNIX)
    console.reset()
    // Let's reset out to be sure to have nothing in it
    outbaos.reset()
    when:
    logger.info("This @|blue is|@ a test")
    then:
    assert AnsiRenderer.render("This @|blue is|@ a test\n").contentEquals(outbaos.toString())
  }

  def "Info without ANSI"() {
    setup:
    // Let's force to not have an ANSI enable console
    System.setProperty(TerminalFactory.JLINE_TERMINAL, TerminalFactory.OFF)
    console.reset()
    // Let's reset out to be sure to have nothing in it
    outbaos.reset()
    when:
    logger.info("This @|blue is|@ a test")
    then:
    assert "This is a test\n".contentEquals(outbaos.toString())
  }

  def "Warn with ANSI"() {
    setup:
    // Let's force to have an ANSI enable console
    System.setProperty(TerminalFactory.JLINE_TERMINAL, TerminalFactory.UNIX)
    console.reset()
    // Let's reset out to be sure to have nothing in it
    outbaos.reset()
    when:
    logger.warn("This @|blue is|@ a test")
    then:
    assert AnsiRenderer.render("@|${Logger.Level.WARN.color} ${Logger.Level.WARN.prefix}This is a test|@\n").contentEquals(
        outbaos.toString())
  }

  def "Warn without ANSI"() {
    setup:
    // Let's force to not have an ANSI enable console
    System.setProperty(TerminalFactory.JLINE_TERMINAL, TerminalFactory.OFF)
    console.reset()
    // Let's reset out to be sure to have nothing in it
    outbaos.reset()
    when:
    logger.warn("This @|blue is|@ a test")
    then:
    assert "[WARN]  This is a test\n".contentEquals(outbaos.toString())
  }

  def "Error with ANSI"() {
    setup:
    // Let's force to have an ANSI enable console
    System.setProperty(TerminalFactory.JLINE_TERMINAL, TerminalFactory.UNIX)
    console.reset()
    // Let's reset out to be sure to have nothing in it
    outbaos.reset()
    when:
    logger.error("This @|blue is|@ a test")
    then:
    assert AnsiRenderer.render("@|${Logger.Level.ERROR.color} ${Logger.Level.ERROR.prefix}This is a test|@\n").contentEquals(
        outbaos.toString())
  }

  def "Error without ANSI"() {
    setup:
    // Let's force to not have an ANSI enable console
    System.setProperty(TerminalFactory.JLINE_TERMINAL, TerminalFactory.OFF)
    console.reset()
    // Let's reset out to be sure to have nothing in it
    outbaos.reset()
    when:
    logger.error("This @|blue is|@ a test")
    then:
    assert "[ERROR] This is a test\n".contentEquals(outbaos.toString())
  }
}
