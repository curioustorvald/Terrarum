## How To Edit the Graaljs Jars

0. Download following from Maven:

    org.graalvm.js:js:00.0.0
    org.graalvm.js:js-scriptengine:00.0.0

1. grab `js-00.0.0.jar`
2. on `META-INF/services/com.oracle.truffle.api.TruffleLanguage$Provider`, edit as shown:

    com.oracle.truffle.js.lang.JavaScriptLanguageProvider (existing line)
    com.oracle.truffle.regex.RegexLanguageProvider        (<< add this line)

3. grab `regex-00.0.0.jar`
4. on `META-INF/services/com.oracle.truffle.api.TruffleLanguage$Provider`, edit as shown:

    com.oracle.truffle.regex.RegexLanguageProvider        (existing line)
    com.oracle.truffle.js.lang.JavaScriptLanguageProvider (<< add this line)

5. Re-zip two files
