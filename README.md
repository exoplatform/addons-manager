eXo platform add-ons manager
==============

## DESCRIPTION

Command line tool for eXo platform 4.x to install/uninstall add-ons

## LICENSE

[LGPLv3](http://www.gnu.org/licenses/lgpl.html)

## SYSTEM REQUIREMENTS

- [Java](http://www.oracle.com/technetwork/java/javase/downloads/) 6+ (Build & Run)
- [Apache Maven](http://maven.apache.org) 3.0.4+ (Build)
- [eXo platform](http://www.exoplatform.org/) 4.0.0+ (Run)

## QUICKSTART

    git clone git@github.com:exoplatform/addons-manager.git && mvn package

Unpack the content of the generated archive ```target/addons-manager-VERSION.zip``` into you eXo platform installation directory
and then use the script ```addon.bat``` on windows systems and ```addon.sh``` on linux/unix systems.

## Usage

We are using ```addon.sh``` in our samples. If you are on a windows system, just use ```addon.bat``` instead.

Display all available addons :

    addon.sh --list

Display all available addons including development versions (snapshots) :

    addon.sh --list --snapshots

Install the latest stable version of the add-on ```foo```

    addon.sh --install foo

Install the latest stable or development version of the add-on ```foo```

    addon.sh --install foo --snapshots

Install the version ```42.0``` of the add-on ```foo```

    addon.sh --install foo:42.0

Enforce to reinstall the latest stable version of the add-on ```foo```

    addon.sh --install foo --force

Uninstall the add-on ```foo```

    addon.sh --uninstall foo

## BUILD (AND AUTOMATED TESTS)

Continuous Integration (Unit tests + deployment in maven repository) : [![Build Status](https://ci.exoplatform.org/buildStatus/icon?job=addons-manager-master-ci)](https://ci.exoplatform.org/job/addons-manager-master-ci/)

Integration Tests : [![Build Status](https://ci.exoplatform.org/buildStatus/icon?job=addons-manager-master-ci)](https://ci.exoplatform.org/job/addons-manager-master-ci/)

To build the project you launch

    mvn verify

You can additionally activate the execution of integration tests with

    mvn verify -Prun-its

To deactivate all automated tests

    mvn verify -DskipTests
