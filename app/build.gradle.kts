android {
    namespace = "com.simpleproxy"
    compileSdk = 33  // 从34改成33
    ...
    defaultConfig {
        ...
        targetSdk = 33  // 从34改成33
    }
    ...
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.8"  // 从1.5.4改成1.4.8
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")  // 从1.12.0改成1.10.1
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")  // 从2.6.2改成2.6.1
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.compose.ui:ui:1.4.3")  // 从1.5.4改成1.4.3
    implementation("androidx.compose.ui:ui-graphics:1.4.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.3")
    implementation("androidx.compose.material3:material3:1.1.0")  // 从1.1.2改成1.1.0
    implementation("androidx.activity:activity-compose:1.7.2")  // 从1.8.1改成1.7.2
}
