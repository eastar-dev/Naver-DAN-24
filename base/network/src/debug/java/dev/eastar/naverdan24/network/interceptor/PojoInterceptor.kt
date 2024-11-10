package dev.eastar.naverdan24.network.interceptor

import android.util.Log
import androidx.core.text.isDigitsOnly
import dev.eastar.naverdan24.network.initializer.stringClone
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import retrofit2.Invocation
import wu.seal.jsontokotlin.library.JsonToKotlinBuilder

/**
 * Created by eastar on 2024/06/08
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Pojo(
    val value: String = "",
)

class PojoInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val pojo = request.tag(Invocation::class.java)?.method()?.getAnnotation(Pojo::class.java) == null
        if (pojo)
            return response
        val body = response.body ?: return response
        val contentType: MediaType? = body.contentType()
        val isJson = contentType?.subtype.toString().lowercase() == "json"
        if (!isJson) return response
        val json = body.stringClone()

        Log.i("tag", "---------------------------------------")
        val clzName = request.tag(Invocation::class.java)?.method()?.getAnnotation(Pojo::class.java)?.value?.takeIf { it.isNotBlank() }
            ?: request.url.pathSegments
                .asSequence()
                .drop(2)
                .filterNot { it.isBlank() }
                .filterNot { it.isDigitsOnly() }
                .filterNot { it.matches("^(?:[a-zA-Z]{0,3}\\d{1,2}|\\d+)\$".toRegex()) }
                .map { it.replace("\\.json\$|\\.xml\$".toRegex(), "") }
                .map { it.toPascalCase() }
                .joinToString("", postfix = "ApiModel")

        generatorPojo(clzName) { json }
        Log.i("tag", "---------------------------------------")
        return response
    }
}

private fun generatorPojo(name: String, json: () -> String): String = JsonToKotlinBuilder()
    .setPackageName("com.my.package.name")

    //.enableVarProperties(false) // optional, default : false
    //.setPropertyTypeStrategy(PropertyTypeStrategy.AutoDeterMineNullableOrNot) // optional, default :  PropertyTypeStrategy.NotNullable
    .setDefaultValueStrategy(wu.seal.jsontokotlin.model.DefaultValueStrategy.AvoidNull) // optional, default : DefaultValueStrategy.AvoidNull

    .setAnnotationLib(wu.seal.jsontokotlin.model.TargetJsonConverter.Serializable) // optional, default: TargetJsonConverter.None

    //.enableComments(true) // optional, default : false
    //.enableOrderByAlphabetic(true) // optional : default : false
    .enableInnerClassModel(true) // optional, default : false
    //.enableMapType(true)// optional, default : false
    .enableCreateAnnotationOnlyWhenNeeded(true) // optional, default : false
    //.setIndent(4)// optional, default : 4
    //.setParentClassTemplate("android.os.Parcelable") // optional, default : ""

    //.enableKeepAnnotationOnClass(true) // optional, default : false
    //.enableKeepAnnotationOnClassAndroidX(true) // optional, default : false
    //.enableAnnotationAndPropertyInSameLine(true) // optional, default : false
    //.enableParcelableSupport(true) // optional, default : false
    //.setPropertyPrefix("MyPrefix") // optional, default : ""
    //.setPropertySuffix("MySuffix") // optional, default : ""
    //.setClassSuffix("MyClassSuffix")// optional, default : ""
    .enableForcePrimitiveTypeNonNullable(true) // optional, default : false
    //.enableForceInitDefaultValueWithOriginJsonValue(true) // optional, default : false
    .build(json(), name) // finally, get KotlinClassCode string
    .also {
        Log.i("tag", it)
    }


/**
 * 함수음절 부분을 나누는 함수
 * ThisIsTestCase -> [This, Is, Test, Case]
 */
private fun String.splitCase(): List<String> = split("[._ -]".toRegex()).map { it.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])".toRegex()) }.flatten()

private fun String.toCamelCase(): String = toPascalCase().replaceFirstChar { it.lowercase() }

private fun String.toPascalCase(): String = splitCase().joinToString("") {
    it.lowercase().replaceFirstChar { it.titlecase() }
}
