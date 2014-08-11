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
package org.exoplatform.platform.am

import spock.lang.Subject

import static org.exoplatform.platform.am.cli.CommandLineParameters.getHELP_LONG_OPT

/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
@Subject(AddonsManager)
class AddonsManagerIT extends IntegrationTestsSpecification {

  def cleanup() {
    // After each test we remove the content of the add-ons directory to be safe
    getEnvironmentSettings().getAddonsDirectory().deleteDir()
  }

  def "Without any param the program must return an error"() {
    expect:
    AddonsManagerConstants.RETURN_CODE_INVALID_COMMAND_LINE_PARAMS == launchAddonsManager([""]).exitValue()
  }

  def "[AM_CLI_02] With --help param the program must display the help"() {
    expect:
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([HELP_LONG_OPT]).exitValue()
  }

}
