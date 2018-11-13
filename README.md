# gradle-plugin-classycle

This is a gradle plugin for the java **classycle dependency checker**.
The current version works with the latest stable version **1.4.2**
from the [original homepage](http://classycle.sourceforge.net/).


This plugin needs the gradle **javaPlugin to be loaded first**.


Per default this plugin expects a dependency definition file at
**etc/classycle/dependencies.ddf** and creates a gradle task **classycleMain**
for the main java sourceSet.


Of course you can set the location of the definition file and scan other
sourceSets as well (see usage-section below).


For every sourceSet specified this plugin will create its own task,
named **classycle<>SourceSetName<>**. These tasks depend on the execution of
the related "classes" task (e.g. **classycleMain depends classes** and
**classycleTest depends testClasses**).


For convenience reasons **an additional
task classycle is created** which executes every single sourceSet-specific
classycle task.


The Java **check task is altered and depends on the classycle task**.




## Usage
```groovy
plugins {
    id 'java'
    id 'de.otto.classycle' version '1.3'
}

// if you want to override the default config, otherwise you can omit this section
classycleConfig {
    definitionFile = 'etc/classycle/dependencies.ddf'
    sourceSets = [project.sourceSets.main]
}
```


## Example definition file

A definition file for a spring-boot based microserive might look like this
```
# Base packages
#----------------------------------------------
{root} = com.example.project.root.package.path
[root] = ${root}.*

# Independence of Architecture-Layers
#----------------------------------------------
[domain] = ${root}.domain.*
[service] = ${root}.service.*
[web] = ${root}.web.*

check sets [domain] [service] [web]

check [domain] independentOf [service]
check [domain] independentOf [web]

check [service] independentOf [web]
```


## Acknowledgments

The Classycle Dependency Checker was created by Franz-Josef Elmer. 
Read more about it at http://classycle.sourceforge.net/.

This plugin was insprired by the Classycle Gradle Plugin by Konrad Garus
https://github.com/konrad-garus/classycle-gradle-plugin

