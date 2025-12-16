# File Permission Preservation Implementation

## Overview
Added functionality to preserve file execution permissions and other POSIX attributes when copying files on macOS (and other Unix-like systems).

## Changes Made

### 1. New Methods Added to `FileUtils.java`

#### `preserveFilePermissions(String sourceFile, String targetFile)`
- Preserves file permissions from source to target file
- Works on POSIX-compliant systems (macOS, Linux, Unix)
- Falls back to basic `setExecutable()`, `setReadable()`, `setWritable()` on Windows
- Supports all permission types: owner, group, and others (read, write, execute)

#### `preserveZipEntryPermissions(ZipEntry zipEntry, File targetFile)`
- Extracts Unix permissions from ZIP entry external file attributes
- Applies permissions to extracted files
- Handles ZIP files created on Unix systems with permission metadata
- Uses reflection for Java 8 compatibility

#### `storeFilePermissionsInZipEntry(Path filePath, ZipEntry zipEntry)`
- Stores file permissions in ZIP entry when creating archives
- Embeds Unix permissions in external file attributes
- Ensures permissions are preserved through ZIP compression/extraction cycle
- Uses reflection for Java 8 compatibility

#### `permissionsFromUnixMode(int mode)`
- Converts Unix octal permission mode (e.g., 0755, 0644) to Java's `PosixFilePermission` set
- Helper method for reading permissions from ZIP entries

#### `unixModeFromPermissions(Set<PosixFilePermission> permissions)`
- Converts Java's `PosixFilePermission` set to Unix octal permission mode
- Helper method for storing permissions in ZIP entries

### 2. Integration Points

#### File Copy Operation
After copying files, the `copyFile()` method now calls:
```java
preserveFilePermissions(source.getAbsolutePath(), target.getAbsolutePath());
```

#### ZIP Compression
When creating ZIP files, the `MyZipFileVisitor.visitFile()` method now calls:
```java
storeFilePermissionsInZipEntry(file, ze);
```

#### ZIP Extraction
When extracting ZIP files, the `unzipFile()` method now calls:
```java
preserveZipEntryPermissions(zipEntry, destFile);
```

## Technical Details

### Java 8 Compatibility
The implementation uses reflection to access `ZipEntry.getExternalFileAttributes()` and `ZipEntry.setExternalFileAttributes()` methods, which were added in Java 13. This ensures the code compiles and runs on Java 8 while taking advantage of newer features when available.

### POSIX vs Non-POSIX Systems
- On POSIX systems (macOS, Linux): Full permission preservation including owner, group, and others
- On non-POSIX systems (Windows): Basic executable, readable, writable flags only

### Permission Format in ZIP Files
ZIP files store Unix permissions in the high 16 bits of the external file attributes field:
- Format: `(unixMode << 16) | 0x8000`
- Example: 0755 permissions → `(0755 << 16) | 0x8000 = 0x81ED8000`

### Error Handling
All permission-related operations are wrapped in try-catch blocks and log errors at the debug level, ensuring that permission preservation failures don't interrupt the copy/extract operations.

## Testing

### Manual Testing
Use the provided `test-permissions.sh` script to create test files with different permissions:

```bash
./test-permissions.sh
```

This creates:
- `executable-script.sh` (755 permissions)
- `regular-file.txt` (644 permissions)
- `readonly-file.txt` (444 permissions)

### Verification
After copying files with FastCopy, compare permissions:
```bash
ls -l /tmp/fastcopy-permission-test/source/
ls -l /tmp/fastcopy-permission-test/target/
```

Or use octal format:
```bash
stat -f "%A %N" /tmp/fastcopy-permission-test/source/*
stat -f "%A %N" /tmp/fastcopy-permission-test/target/*
```

## Benefits

1. **Executable Scripts**: Shell scripts, Python scripts, and other executables maintain their execute permissions
2. **Security**: Read-only files remain read-only, preserving security configurations
3. **Compliance**: Maintains file permissions required by certain applications or deployment processes
4. **Cross-Platform**: Works seamlessly on macOS, Linux, and Unix systems
5. **Backward Compatible**: Works with Java 8+ and gracefully degrades on systems without POSIX support

## Notes

- Permission preservation is automatic and requires no user configuration
- Verbose mode (`-v` flag) will show detailed permission information in debug logs
- The feature respects the file system capabilities (POSIX vs non-POSIX)
- ZIP file permission storage follows the standard ZIP specification for Unix attributes
