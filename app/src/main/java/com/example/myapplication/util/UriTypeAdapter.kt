package com.example.myapplication.util // Or a suitable package

import android.net.Uri
import com.google.gson.*
import java.lang.reflect.Type

class UriTypeAdapter : JsonSerializer<Uri>, JsonDeserializer<Uri> {
    override fun serialize(
        src: Uri?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.toString())
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Uri? {
        return json?.asString?.let { Uri.parse(it) }
    }
}