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

## RESOURCES

- [Issues Tracker](https://jira.exoplatform.org/browse/AM/)
- [Continuous Integration Job (Unit tests + deployment in maven repository) ![Build Status](https://ci.exoplatform.org/buildStatus/icon?job=addons-manager-master-ci)](https://ci.exoplatform.org/job/addons-manager-master-ci/)
- [Integration Tests Validation Job ![Build Status](https://ci.exoplatform.org/buildStatus/icon?job=addons-manager-master-ci)](https://ci.exoplatform.org/job/addons-manager-master-ci/)
- [Reporting Job (Sonar + Maven website) ![Build Status](https://ci.exoplatform.org/buildStatus/icon?job=addons-manager-master-reporting)](https://ci.exoplatform.org/job/addons-manager-master-reporting/)
- [Sonar Quality dashboard](https://sonar.exoplatform.org/dashboard/index/org.exoplatform.platform:addons-manager)
- [Maven website](https://projects.exoplatform.org/addons-manager/)
- Maven artifacts
  - [Snapshots](https://repository.exoplatform.org/content/repositories/exo-snapshots/org/exoplatform/platform/addons-manager/)
  - [Releases](https://repository.exoplatform.org/content/repositories/exo-releases/org/exoplatform/platform/addons-manager/)
- [Specifications](http://community.exoplatform.com/portal/intranet/wiki/group/spaces/platform_41/Add-ons_Manager)

## QUICKSTART

    git clone git@github.com:exoplatform/addons-manager.git && mvn package

Unpack the content of the generated archive ```target/addons-manager-VERSION.zip``` into you eXo platform installation directory
and then use the script ```addon.bat``` on windows systems and ```addon``` on linux/unix systems.

## Usage

We are using ```addon``` in our samples. If you are on a windows system, just use ```addon.bat``` instead.

Display all available addons :

    addon list

Display all available addons including development versions (snapshots) :

    addon list --snapshots

Display all available addons including unstable versions (alpha, beta, ...) :

    addon list --unstable

Display all installed addons in your platform server :

    addon list --installed

Display all installed addons with an existing more recent stable version :

    addon list --outdated

Display all installed addons with an existing more recent stable or snapshot version :

    addon list --outdated  --snapshots

Display all installed addons with an existing more recent stable or unstable version :

    addon list --outdated --unstable

Install the latest stable version of the add-on ```foo```

    addon install foo

Install the latest stable or development version of the add-on ```foo```

    addon install foo --snapshots

Install the latest stable or unstable version of the add-on ```foo```

    addon install foo --unstable

Install the version ```42.0``` of the add-on ```foo```

    addon install foo:42.0

Enforce to reinstall the latest stable version of the add-on ```foo```

    addon install foo --force
    
Install the local patch  ```patch-plfversion.zip.enc```
    
    addon apply ../../patchfolderpath/patch-plfversion.zip.enc
        
Install with CRC Verification of the patch```patch-plfversion.zip.enc``` 
    
    addon apply ../../patchfolderpath/patch-plfversion.zip.enc --crc xxxxxxxxxxxxx

Uninstall the add-on ```foo```

    addon uninstall foo

## BUILD (AND AUTOMATED TESTS)

To build the project you launch

    mvn verify

You can additionally activate the execution of integration tests with

    mvn verify -Prun-its

To deactivate all automated tests

    mvn verify -DskipTests
