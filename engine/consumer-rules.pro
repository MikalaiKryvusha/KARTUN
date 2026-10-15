# R8 keep rules of the native engine, shipped to every app that uses this module (consumerProguardFiles).
# The C++ side calls back into Kotlin by NAME: kartun_engine.cpp looks up `onTile(II)Z` with GetMethodID on the listener's
# class, so neither the interface method nor its implementations may be renamed or removed. The `native` methods of
# NcnnUpscaler are kept by the default Android rules (-keepclasseswithmembernames … native <methods>).
-keep interface com.kriniks.kartun.engine.TileListener { *; }
-keepclassmembers class * implements com.kriniks.kartun.engine.TileListener {
    boolean onTile(int, int);
}
