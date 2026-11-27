// Signs release builds with the key named in ~/.gradle/gradle.properties (DUMSCRL_STORE_FILE, DUMSCRL_STORE_PASSWORD,
// DUMSCRL_KEY_ALIAS, DUMSCRL_KEY_PASSWORD), which stays out of git. Without those properties it falls back to the
// debug key, so anyone can still build. A plugin because `expo prebuild --clean` rewrites android/app/build.gradle.
const { withAppBuildGradle } = require('expo/config-plugins');

const RELEASE = `
        release {
            if (findProperty('DUMSCRL_STORE_FILE')) {
                storeFile file(findProperty('DUMSCRL_STORE_FILE'))
                storePassword findProperty('DUMSCRL_STORE_PASSWORD')
                keyAlias findProperty('DUMSCRL_KEY_ALIAS')
                keyPassword findProperty('DUMSCRL_KEY_PASSWORD')
            }
        }`;

module.exports = (config) =>
  withAppBuildGradle(config, (c) => {
    let g = c.modResults.contents;
    if (g.includes('DUMSCRL_STORE_FILE')) return c;
    const signing = 'signingConfigs {';
    // The release build type: the debug-key line right after Expo's "In production" comment.
    const release = /(you need to generate your own keystore file\.[^\n]*\n[^\n]*\n\s*)signingConfig signingConfigs\.debug/;
    if (!g.includes(signing) || !release.test(g)) throw new Error('withReleaseSigning: build.gradle layout changed; update the plugin');
    g = g.replace(release, "$1signingConfig findProperty('DUMSCRL_STORE_FILE') ? signingConfigs.release : signingConfigs.debug");
    g = g.replace(signing, signing + RELEASE);
    c.modResults.contents = g;
    return c;
  });
