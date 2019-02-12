rm app/build/outputs/apk/debug/app-debug.apk ;JAVA_HOME=/opt/android-studio/jre/ ./gradlew build && adb uninstall org.cpera.apmng && adb install app/build/outputs/apk/debug/app-debug.apk

