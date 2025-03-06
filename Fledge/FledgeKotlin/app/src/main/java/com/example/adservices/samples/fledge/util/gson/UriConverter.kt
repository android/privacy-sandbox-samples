package com.example.adservices.samples.fledge.util.gson

import android.net.Uri
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class UriConverter : JsonSerializer<Uri>, JsonDeserializer<Uri> {
  override fun serialize(
    src: Uri?,
    typeOfSrc: Type?,
    context: JsonSerializationContext?,
  ): JsonElement {
    return JsonPrimitive(src.toString())
  }

  override fun deserialize(
    json: JsonElement?,
    typeOfT: Type?,
    context: JsonDeserializationContext?,
  ): Uri? {
    return json?.asString?.let { Uri.parse(it) }
  }
}