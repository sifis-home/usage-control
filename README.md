[![coverage](https://raw.githubusercontent.com/sifis-home/usage-control/gh-pages/reports/jacoco.svg 'Code Coverage')](https://sifis-home.github.io/usage-control/reports/index.html)

# Usage Control (UCS)

This repository (branch sifis-home) implements the Usage Control System for the SIFIS-Home project.

The repo on GitHub at [https://github.com/sifis-home/usage-control](https://github.com/sifis-home/usage-control) is a "push mirror" of the original repository on a CNR GitLab instance at [https://sssg-dev.iit.cnr.it/marco-rasori/new-ucs](https://sssg-dev.iit.cnr.it/marco-rasori/new-ucs).

Being a push mirror, all the changes have to be done at the repository hosted on the GitLab instance.

# Building

You need Java8 and [maven](https://maven.apache.org), you may install it using [sdkman](https://sdkman.io):

```sh
# Pick a java8 distribution
$ sdk list java | grep 8.0
$ sdk install java 8.0.{minor}-{dist}
# The latest maven is fine to use
$ sdk install maven
```

Then you may build the package normally
```sh
$ mvn clean package
```
