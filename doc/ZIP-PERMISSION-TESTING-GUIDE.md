# ZIP File Permission Preservation Testing Guide

## Overview
This document describes the comprehensive unit tests for verifying file permission preservation through ZIP compression and extraction operations.

## Test File Location
```
src/test/java/org/mhisoft/fc/ZipFilePermissionPreservationTest.java
```

## Running the Tests

### Run Only Permission Tests
```bash
mvn test -Dtest=ZipFilePermissionPreservationTest
```

### Run All Tests
```bash
mvn test
```

### Run a Specific Test Method
```bash
mvn test -Dtest=ZipFilePermissionPreservationTest#testZipPreservesExecutablePermissions
```

## Test Cases

### 1. Executable Permissions Test
**Test Method**: `testZipPreservesExecutablePermissions()`

**What it tests**:
- Creates a shell script with executable permissions (rwxr-xr-x / 755)
- Compresses it into a ZIP using `compressDirectory()`
- Extracts it using `unzipFile()`
- Verifies permissions are exactly preserved

**Expected Result**: Extracted file has identical permissions as source

---

### 2. Read-Only Permissions Test
**Test Method**: `testZipPreservesReadOnlyPermissions()`

**What it tests**:
- Creates a file with read-only permissions (r--r--r-- / 444)
- Performs ZIP compression and extraction round-trip
- Verifies file remains read-only

**Expected Result**: Extracted file is not writable

---

### 3. Various Permission Combinations Test
**Test Method**: `testZipPreservesVariousPermissions()`

**What it tests**:
- Creates 4 files with different permission patterns:
  - `file-755.txt`: rwxr-xr-x (executable)
  - `file-644.txt`: rw-r--r-- (standard file)
  - `file-600.txt`: rw------- (owner-only access)
  - `file-777.txt`: rwxrwxrwx (world-writable)
- Compresses all into one ZIP
- Extracts and verifies each file's permissions

**Expected Result**: All 4 files preserve their unique permissions

---

### 4. Subdirectory Permissions Test
**Test Method**: `testZipPreservesPermissionsInSubdirectories()`

**What it tests**:
- Creates nested directory structure:
  ```
  testDir/
    subdir1/
      script.sh (rwxr-xr-x)
      subdir2/
        config.conf (rw-r-----)
  ```
- Compresses with `recursive=true`
- Verifies permissions preserved in nested structure

**Expected Result**: Permissions preserved at all directory levels

---

### 5. Windows Fallback Test
**Test Method**: `testWindowsFallbackPreservesExecutable()`

**What it tests**:
- Uses platform-independent `File.setExecutable()` method
- Works on both POSIX and Windows systems
- Verifies basic executable flag preservation

**Expected Result**: Works on all platforms including Windows

## Platform Compatibility

### POSIX Systems (Linux, macOS, Unix)
- Full POSIX permission support (owner/group/other, read/write/execute)
- Tests check exact permission strings (e.g., "rwxr-xr-x")
- All 5 tests run fully

### Windows Systems
- Tests 1-4 are gracefully skipped with informational message
- Test 5 runs and verifies basic executable flag
- Uses fallback mechanism in FileUtils

### Detection Logic
```java
try {
    isPosixSupported = Files.getFileStore(testDir)
        .supportsFileAttributeView("posix");
} catch (Exception e) {
    isPosixSupported = false;
}
```

## Implementation Details

### Storage Mechanism
Permissions are stored in ZIP entry extra field:
- **Header ID**: 0x504D ("PM" = Permission Mode)
- **Format**: 
  - 2 bytes: Header ID
  - 2 bytes: Data size (4)
  - 4 bytes: Unix permission mode in little-endian

### Methods Tested
1. `FileUtils.compressDirectory()` - Stores permissions when creating ZIP
2. `FileUtils.unzipFile()` - Restores permissions when extracting ZIP
3. `FileUtils.storeFilePermissionsInZipEntry()` - Internal method to write permissions
4. `FileUtils.preserveZipEntryPermissions()` - Internal method to read permissions

## Test Output Example

```
=== Test: ZIP Preserves Executable Permissions ===

Source file permissions: rwxr-xr-x
ZIP created: _fastcopy_auto_create_zipPermTest6014477707185627617.zip
Extracted to: /tmp/zipExtract3937015077503443440
Extracted file permissions: rwxr-xr-x
✓ Executable permissions preserved successfully!
```

## Verification
Tests use JUnit assertions to verify:
- Files exist after extraction
- Permissions match exactly (using `PosixFilePermissions.toString()`)
- Platform-specific flags work (`canExecute()`, `canWrite()`)

## Cleanup
Each test automatically cleans up:
- Temporary test directories
- Created ZIP files
- Extracted files

Cleanup is performed in the `@After` method (`tearDown()`).

## Debugging

### Enable Verbose Output
Tests already have verbose mode enabled:
```java
RunTimeProperties.instance.setVerbose(true);
```

This shows debug messages about permission storage/retrieval.

### Check ZIP Contents
To manually inspect a ZIP file created during tests:
```bash
# View ZIP entry extra data
unzip -l _fastcopy_auto_create_*.zip

# Extract to specific directory
unzip _fastcopy_auto_create_*.zip -d test-extract/
```

## Integration with CI/CD

These tests are part of the standard Maven test suite and will run automatically in CI/CD pipelines:

```yaml
# Example GitHub Actions
- name: Run tests
  run: mvn test
```

## Success Criteria

All tests pass when:
- ✅ 5/5 tests pass
- ✅ 0 failures
- ✅ 0 errors
- ✅ Platform-specific tests skip gracefully when feature not supported
- ✅ Time < 1 second (typical: ~200ms)

## Troubleshooting

### "Skipping test - POSIX permissions not supported"
- **Cause**: Running on Windows or file system doesn't support POSIX
- **Solution**: This is expected behavior, test 5 should still pass

### Permission mismatch after extraction
- **Cause**: May indicate bug in storage/retrieval implementation
- **Check**: Verify `storeFilePermissionsInZipEntry()` and `preserveZipEntryPermissions()` methods

### ZIP file not created
- **Cause**: May indicate issue with `compressDirectory()` method
- **Check**: Verify source directory exists and is accessible
