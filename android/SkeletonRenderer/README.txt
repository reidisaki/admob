This is a skeleton renderer to help you build a FreeWheel compatible renderer from scratch.

Here are the setup steps:
1. Make sure your Android SDK Tools is up to date. This package is validated on Android SDK Tools 20.0.1.
2. Copy the local.example.properties to local.properties, and modify properties in it as necessary. 'sdk.dir' is a property you probably need to change.
3. Unpack FreeWheel Android AdManager package, put FWAdManager.jar in 'libs' directory and freewheel.properties in the root directory of this sample.
4. Run 'ant build' to build the renderer. Find the renderer JAR file in 'build' folder.

Notes:
After successfully building this skeleton renderer, you can rename it to your own renderer project. Here are a few steps to follow:
  a. Change the class name SkeletonRenderer and its package name in all source codes.
  b. Change the folder names by your new package name.
  c. Change the class name and package name in all build scripts and property files.
