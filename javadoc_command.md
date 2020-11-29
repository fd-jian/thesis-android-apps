# Generate Javadoc with Links to Android Classes

- This is a scripted solution. Another option may be the `gradle-android-javadoc-plugin`, but the last release was in July 2018, and the repository is stale since June 2019.

## Generate package-list for Android SDK Sources
- `{@link ...}` in javadoc does not work out of the box with android libraries.
- Also, `-link https://developer.android.com/reference` provided on the command line does not work
- Javadoc cannot find the `package-list` file in the online reference. Therefore, a `package-list` file must be generated and be provided with `-linkoffline`
- Generate the `package-list`:

```sh
# generate_pkglist.sh
---
#!/bin/sh

SDK_VERSION=$1
ANDROID_HOME=${2:-${ANDROID_HOME:-/opt/android-sdk}}

DOC_DIR=$ANDROID_HOME/doc/android-$SDK_VERSION

mkdir -p "$DOC_DIR" &&
(
  cd "$ANDROID_HOME"/sources/android-"$SDK_VERSION" &&
  find . -type d -exec sh -c \
    '[ -n "$(find "$1"/. ! -name . -prune -type f -name "*.java")" ]' \_ {} \; \
    -print |
  sed 's|^./||g;s|/|.|g'
) > "$DOC_DIR"/package-list
```
- Example: `./generate_pkglist.sh 29 /opt/android-sdk`
- Second parameter `/opt/android-sdk` may be ommited, it's the default.

## Generate package-list for Google GMS Play Service Libraries
- Download necessary JARs or find them in gradle cache directory
  - Android studio may be used to find out the JAR filepaths
- Example: Google Play Services Wearable
  - JAR was found in `~/.local/share/gradle/caches/transforms-2/files-2.1/2861b1f4700149d2358236857a33bf65/jetified-play-services-wearable-17.0.0/jars/classes.jar`
```sh
# generate_google_pkglist.sh
---
#!/bin/sh

DOC_DIR=/opt/googleplay/doc
cp "$1" /tmp/classes.jar &&
mkdir -p "$DOC_DIR" &&
mkdir -p /tmp/googleplay &&
unzip -o /tmp/googleplay /tmp/classes.jar &&
(
  cd /tmp/googleplay &&
  find . -type d -exec sh -c \
    '[ -n "$(find "$1"/. ! -name . -prune -type f -name "*.class")" ]' _ {} \; \
    -print |
  sed 's|^./||g;s|/|.|g'
) > "$DOC_DIR"
```
  - `./generate_google_pkglist.sh ~/.local/share/gradle/caches/transforms-2/files-2.1/2861b1f4700149d2358236857a33bf65/jetified-play-services-wearable-17.0.0/jars/classes.jar`

## Intellij/Android Studio `Generate Javadoc`
- This only works until JDK 8. `-bootclasspath` is deprecated, no woraround could be found.
- Without `-bootclasspath`, javadoc does not recognize the android library imports and fails.
- How it works:
  - Configure JDK 8 as the project JDK in Intellij
  - Navigate to Tools > Generate Javadoc
  - Use the following command line options:
  ```
  -linkoffline https://developers.google.com/android/reference /opt/googleplay/doc -linkoffline https://developer.android.com/reference /opt/android-sdk/doc/android-29 -bootclasspath /opt/android-sdk/platforms/android-29/android.jar
  ```

