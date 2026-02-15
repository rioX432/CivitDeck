---
name: fix-detekt
description: Run detekt and fix reported issues
user-invocable: true
disable-model-invocation: true
allowed-tools: Bash, Read, Edit
---

# Fix Detekt Issues

Run detekt, parse the output, and fix all reported issues.

## Steps

1. **First run**: Execute `./gradlew detekt` and capture the output.
2. **Parse issues**: Extract file paths, line numbers, and rule violations from the output.
3. **Fix each issue**: Read the file, understand the context, and apply the fix:
   - `LongMethod`: Split the composable/function into smaller pieces
   - `MagicNumber`: Extract to a named constant
   - `UnusedImport`: Remove the import
   - `MaxLineLength`: Break the line appropriately
   - Other rules: Fix according to detekt documentation
4. **Second run**: Run `./gradlew detekt` again — auto-correct may reorder imports, causing new issues on the first pass.
5. **Fix remaining**: If new issues appear from import reordering, fix them.
6. **Final verification**: Run `./gradlew detekt` one more time to confirm zero issues.
