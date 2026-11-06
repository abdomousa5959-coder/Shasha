# شاشة: مشروع أندرويد تجريبي

ده مشروع Android Studio، مش APK جاهز. فيه مشغّل فيديو تجريبي مرفق، واختيار ملف فيديو من الجهاز، وإدخال رابط فيديو مباشر مرخّص عبر HTTPS. التشغيل يعتمد على الصيغ ودعم البث في جهاز أندرويد ومصدر الفيديو. المشغّل مبني على ExoPlayer (Media3 1.4.1) ويدعم روابط HLS (`.m3u8`). المكتبات بتتحمّل من Google Maven عند أول مزامنة، ومتجرّبتش على جهاز حقيقي. صفحات المشاهدة، وروابط HTTP، والمحتوى المحمي بـ DRM غير مدعومة. استخدام رابط لا يتأكد من ملكية الحقوق، فتحقق منها بنفسك.

## المتطلبات
- Android Studio مع Android SDK Platform 35 وJDK 17.
- Gradle 8.9 متثبّت محليًا. المشروع لا يحتوي على Gradle Wrapper.
- اتصال بالإنترنت لأول مزامنة لتنزيل Android Gradle Plugin 8.7.3 وأي مكوّنات SDK ناقصة.
- جهاز أو محاكي Android 6.0 أو أحدث.

## بناء APK للتجربة
1. فكّ الملف المضغوط وافتح مجلد المشروع في Android Studio من File > Open.
2. في إعدادات Gradle اختر Gradle 8.9 المحلي وGradle JDK 17. لو تحتاج الـWrapper، من الطرفية داخل المجلد شغّل `gradle wrapper --gradle-version 8.9` باستخدام Gradle المتثبّت، وبعدها اختَر Gradle Wrapper في إعدادات Android Studio.
3. نفّذ Gradle Sync ووافق على تنزيل أي مكونات مطلوبة.
4. من القائمة Build اختَر Build Bundle(s) / APK(s) ثم Build APK(s). اسم القائمة قد يختلف حسب إصدار Android Studio.
5. هتلاقي النسخة التجريبية غالبًا في `app/build/outputs/apk/debug/app-debug.apk`. انقلها للجهاز وثبّتها للسماح بالتجربة. نسخة debug ليست إصدارًا جاهزًا للنشر.

لو الرابط لا يعمل، جرّب الفيديو المرفق أو ملفًا محليًا، ثم تأكد أن الرابط HTTPS مباشر لفيديو بصيغة يدعمها جهازك، وأن الخادم يسمح بتشغيله. لم يُختبر بناء المشروع على Android Studio أو على جهاز أندرويد في بيئة الإنشاء الحالية.

## GitHub Actions (automatic APK builds)

The repository includes `.github/workflows/android-build.yml`:

- Every push, pull request, or manual run builds a debug APK with JDK 17, Android SDK 35, and Gradle 8.9.
- The workflow validates Gradle configuration, runs available unit tests, and uploads `app-debug.apk` as a 30-day artifact.
- Pushing a tag such as `v1.0.0` also attempts a signed release build and publishes the APK as a GitHub Release asset.

### Enable signed releases

Create an Android signing keystore locally and add these GitHub Actions repository secrets (never commit the keystore or passwords):

- `ANDROID_KEYSTORE_BASE64`: Base64-encoded keystore file.
- `ANDROID_KEYSTORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: key alias.
- `ANDROID_KEY_PASSWORD`: key password.

On Linux/macOS, encode the keystore with `base64 -w 0 release.jks` (on macOS use `base64 release.jks`). Then push a version tag, for example `git tag v1.0.0 && git push origin v1.0.0`.

The debug APK is for testing. A release APK is only produced when the signing secrets are configured correctly.
