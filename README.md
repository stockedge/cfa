# cfa
Class File Analyzer (CFA).
Analyze class files from ConstantPool.


# How to Build

```shell
$ export JAVA_HOME=/path/to/jdk
$ ant
```

# How to use

```shell
$ cd dist
$ java -jar cfa.jar <options> <files (JAR or class file)>
```
# Options

* -h
 * Help message
* -t class1,class2,...
 * Target class.
 * CFA will pick up classes from file list.
* -c class1,class2,...
 * Class filter.
 * CFA will pick up classes which include them in ConstantPool.
* -m method1,method2,...
 * Method filter.
 * CFA will pick up classes which include them in ConstantPool.
* -s
 * Short output.
 * If this option is added, CFA will output class name and file path only.

# License
GNU General Public License v2

