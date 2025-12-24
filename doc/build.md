# Maven Build Issue Fixed - ideauidesigner Plugin Disabled

## Problem

The Maven build was failing with:
```
Failed to execute goal org.codehaus.mojo:ideauidesigner-maven-plugin:1.0-beta-2:javac2
A required class was missing: sun/misc/Resource
```

## Root Cause

The `ideauidesigner-maven-plugin` is an old plugin (version 1.0-beta-2) that depends on internal JDK classes like `sun.misc.Resource`. These classes were:
- Part of the internal Java API in Java 8 and earlier
- Removed in Java 9+ as part of the Java Platform Module System (JPMS)
- Not accessible in modern Java versions (Java 11, 17, 21, etc.)

The plugin is only needed for compiling IntelliJ IDEA `.form` files (GUI forms), not for compiling regular Java code or running tests.

## The Solution

Modified the `pom.xml` to disable the `ideauidesigner-maven-plugin` execution by setting the phase to `none`:

**Change Made:**
```xml
<execution>
    <id>default</id>
    <phase>none</phase>  <!-- Changed from process-classes to none -->
    <goals>
        <goal>javac2</goal>
    </goals>
</execution>
```

This tells Maven to not execute the plugin during any build phase, effectively disabling it.

## Impact

### Positive:
- ✅ Maven builds now work successfully
- ✅ All tests can be compiled and run
- ✅ The project builds without errors
- ✅ Compatible with modern Java versions (11+)

### Considerations:
- If you have IntelliJ IDEA `.form` files that need to be compiled, you should:
    - Use IntelliJ IDEA's built-in form compiler (which works with modern Java)
    - Or manually configure IntelliJ IDEA to compile forms before running Maven
- The UI forms in your project will still work when built from IntelliJ IDEA directly

## Test Results

After the fix, all tests pass successfully:
```
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

All the unit tests we created are now working:
1. ✅ testReadFileContentHashAndHexConversion
2. ✅ testGetHash
3. ✅ testReadFileContentHashConsistency
4. ✅ testCompressDirectory
5. ✅ testCompressDirectoryWithSizeThreshold
6. ✅ testCompressEmptyDirectory
7. ✅ testCompressDirectoryPreservesTimestamps
8. ✅ testUnzipFile
9. ✅ testUnzipFileWithVerification
10. ✅ testUnzipFilePreservesTimestamps
11. ✅ testUnzipEmptyZipFile
12. ✅ testUnzipFileWithNestedDirectories

## Alternative Solutions (if needed in the future)

If you need the UI designer plugin to work:

1. **Use a newer alternative**: Replace with a modern plugin that supports Java 11+
2. **Use IntelliJ's compiler**: Configure IntelliJ IDEA to pre-compile forms
3. **Switch to code-based UI**: Migrate from `.form` files to pure Java Swing code
4. **Use Java 8**: Downgrade to Java 8 (not recommended)

## Files Modified

- **`/Users/I831964/SAPDevelop/projects/mhisoft/fastcopy/pom.xml`**
    - Line 277: Changed execution phase from `process-classes` to `none`
    - Added `<id>default</id>` to the execution

## Running Tests

You can now run tests successfully using:
```bash
mvn test                              # Run all tests
mvn test -Dtest=FileUtilsTest         # Run FileUtilsTest
mvn clean package                      # Build the project
```

The Maven build now works correctly!

## so to build the project
1. build in intelliJ first, with the forms built
2. mvn packge
3. run ant to package the jars, ext and mac app. result is under /dist\
   
