package com.example.adservices.samples.fledge.util.gson

import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class AdTechIdentifierConverter: JsonSerializer<AdTechIdentifier>, JsonDeserializer<AdTechIdentifier> {
    override fun serialize(
        src: AdTechIdentifier?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?,
    ): JsonElement {
        return JsonPrimitive(src.toString())
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?,
    ): AdTechIdentifier? {
        return json?.asString?.let { AdTechIdentifier(it) }
    }
}