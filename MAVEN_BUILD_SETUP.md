# Maven Build Configuration - IntelliJ IDE Compilation Only

## What Changed

The pom.xml has been updated to **skip Maven compilation** completely. Maven will now only package the classes that IntelliJ IDEA has already compiled.

### Changes Made:

1. **maven-compiler-plugin**: Set both `default-compile` and `default-testCompile` phases to `none`
2. **ideauidesigner-maven-plugin**: Already set to phase `none` (skips form compilation)

## How to Build Now

### Step 1: Build in IntelliJ IDEA

You **must** build the project in IntelliJ IDEA first:

**Option A: Build Project** (Recommended)
```
Build → Build Project (⌘F9)
```
This compiles all classes including GUI forms and outputs to `target/classes/`

**Option B: Build Artifacts** (For complete JAR)
```
Build → Build Artifacts... → fastcopy-ui → Build
```
This creates the complete JAR directly in `target/fastcopy-ui.jar`

### Step 2: Package with Maven/Ant

After IntelliJ has compiled the classes:

```bash
# Build UI JAR only (packages IntelliJ-compiled classes)
ant rebuild-ui-jar

# Or full build with console JAR too
ant build

# Or just build Mac app
ant bundle-mac-app
```

## Important Notes

1. **IntelliJ MUST compile first** - Maven won't compile anything now
2. **GUI Forms** - IntelliJ automatically compiles .form files into the .class files
3. **Output Directory** - IntelliJ outputs to `target/classes/` by default, which Maven packages
4. **Fast Builds** - Maven now finishes in ~0.3 seconds since it only packages

## Workflow Summary

```
IntelliJ Build → Maven Package → Ant Bundle
    (⌘F9)      →  (ant build)  → (Mac app ready)
```

## Why This Setup?

- IntelliJ's GUI Designer properly compiles .form files with the classes
- Maven's ideauidesigner-maven-plugin is outdated and unreliable
- This gives you full control over compilation in the IDE
- Maven just becomes a packaging tool

## Testing

After building in IntelliJ and running `ant rebuild-ui-jar`:

```bash
# Test the JAR
java -jar dist/fastcopy-ui.jar

# If it works, build the Mac app
ant bundle-mac-app

# Test the Mac app
open dist/Fastcopy\(1.4.1\).app
```

## Troubleshooting

If you get "contentPane cannot be set to null" error:

1. Verify IntelliJ compiled the project: `Build → Build Project`
2. Check that `target/classes/org/mhisoft/fc/ui/` has .class files
3. Ensure IntelliJ's GUI Designer is enabled (should be by default)
4. Rebuild the project completely: `Build → Rebuild Project`

## Dependencies

Maven still manages all dependencies - these are copied to `dist/lib/`:
- commons-io-2.14.0.jar
- commons-vfs2-2.10.0.jar  
- forms_rt-14.0.jar
- commons-lang3-3.17.0.jar
- And others...

These get bundled into the Mac app automatically.
