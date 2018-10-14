# Maven Cleaner

## Main goal

The main goal of this library is to help you find the cause of this exception :

```
Caused by: java.util.zip.ZipException: invalid LOC header (bad signature)
```

This exception happens if a jar of your project is corrupted. This can be the case when downloading 
dependencies on a unstable network connection. 

## Features

- [x] Detect corrupted dependencies
- [x] Detect dependency without checksums
- [ ] Add program argument to delete corrupted dependencies
- [ ] Format a pom.xml
- [ ] Simplify dependencies tree


## Usage

There is no release yet but you can download the only java file and compile it against a JDK8 at least.

```bash
java MavenCleaner <PATH_TO_YOUR_MAVEN_REPOSITORY>
```

For the moment it justs browse your repository and compare md5/sh1 of the dependencies.