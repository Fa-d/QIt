# R8 rules for the release build. The libraries (Hilt, Room, Media3, kotlinx.serialization,
# Navigation, Wear) ship their own consumer rules, and our serialization always goes through the
# compiler-generated serializers, so nothing of ours needs keeping by name.

# Readable stack traces in Play's crash reports: keep line numbers (the mapping file R8 writes,
# uploaded with the bundle, restores the names) and hide the source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
