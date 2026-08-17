# Token Envoy

Gradle plugin that replaces tokens in **compiled classes** (ASM, after javac) and **resources** (`processResources`).
Source files are never rewritten.

## Apply

```groovy
plugins {
    id 'java'
    id 'com.cleanroommc.tokenenvoy' version '0.1.0'
}
```

## DSL

```groovy
tokenEnvoy {
    // Global: applies to every source set
    set 'VERSION', project.version

    main { // Per-source set: only for the `main` source set in this case
        set 'MOD_ID', project.findProperty('mod_id')
        set file('tokens.properties') // NAME=value properties file
        // resourcesOnly = true // default false. This would skip class rewriting

        classes {
            include '**/Tokens.class', 'com.example.Reference'
            exclude '**/internal/**'
        }
        resources {
            include 'mcmod.info', '**/*.json'
            exclude '**/skip.json'
        }
    }
}
```

In classes and resources the marker is always `@{NAME}`:

```java
public static final String VERSION = "@{VERSION}";
```

```json
{ "version": "@{VERSION}" }
```

The DSL and property files use the bare name only (`VERSION`, not `@{VERSION}`).

Do not pass `provider { project.version }`.
That captures `Project` and breaks the configuration cache.
Token Envoy warns if you do.
Prefer a realized value or a Gradle-managed provider:

```groovy
set 'VERSION', project.version
set 'VERSION', providers.gradleProperty('mod_version')
```

### Property files

```properties
# tags.properties
VERSION=${mod_version}
MOD_ID=${mod_id}
```

Keys are token names. Values may use `${gradleProperty}` from `gradle.properties` / `-P`.
Source-set `set` calls override globals of the same name.

### Targets

| Target                                       | How                                                                                                                                                                                                   |
|----------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Java (Groovy/Scala/Kotlin JVM if applicable) | Compile writes raw classes, then `tokenEnvoy<Language>Classes` rewrites `@{NAME}` in string constants, `static final` field values, annotation values, and `invokedynamic` bootstrap strings with ASM |
| Resources                                    | `processResources` filters `@{NAME}` in text files. Known binaries (png, ogg, jar, …) are copied as-is.                                                                                               |

`resourcesOnly = true` on a source set hooks only that source set's `processResources` task.

### File Filters

`classes` and `resources` choose which files receive replacements.
Files that do not match are still compiled or copied and their `@{NAME}` markers stay in place.

Patterns are Gradle Ant-style (`*`, `**`, `?`), relative to the class or resource output root.

- A name without `/` or `**` matches only at that root (`mcmod.info`, `Tags.class`)
- Use `**/Tags.class` to match that file name in any directory
- Paths (`com/example/Reference.class`) match that relative path
- Class patterns also accept a fully-qualified name (`com.example.Tokens`)
- Empty includes mean every file; any matching exclude wins
- Global filters are union'd with the source set's filters

```groovy
tokenEnvoy {
    includeClasses '**/Tags.class'
    includeResources 'mcmod.info', '**/*.json'
    excludeResources '**/skip.json'
}
```

Use `sourceSets.*.output`/`classes` task as the classes input.
The compile task's own destination is a directory, allowing later token changes to be re-applied without recompiling.
