# Keep Room entities and workers. Compose is reflection-free.
-keep class com.jackson4rocks.evexia.data.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { <init>(); }

# pdfbox-android's JPXFilter optionally calls the JP2 decoder, which is not
# bundled; it's only needed for JPEG-2000 images inside PDFs.
-dontwarn com.gemalto.jp2.JP2Decoder
