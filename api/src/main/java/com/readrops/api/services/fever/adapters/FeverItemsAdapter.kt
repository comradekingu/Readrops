package com.readrops.api.services.fever.adapters

import android.annotation.SuppressLint
import com.readrops.api.utils.exceptions.ParseException
import com.readrops.api.utils.extensions.nextNonEmptyString
import com.readrops.api.utils.extensions.nextNullableString
import com.readrops.api.utils.extensions.skipField
import com.readrops.api.utils.extensions.toBoolean
import com.readrops.db.entities.Item
import com.readrops.db.util.DateUtils
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson

class FeverItemsAdapter {

    @ToJson
    fun toJson(items: List<Item>) = ""

    @SuppressLint("CheckResult")
    @FromJson
    fun fromJson(reader: JsonReader): List<Item> = with(reader) {
        return try {
            val items = arrayListOf<Item>()

            beginObject()
            while (nextName() != "items") {
                skipValue()
            }

            beginArray()
            while (hasNext()) {
                beginObject()

                val item = Item()
                while (hasNext()) {
                    with(item) {
                        when (selectName(NAMES)) {
                            0 -> {
                                remoteId = if (reader.peek() == JsonReader.Token.STRING) {
                                    nextNonEmptyString()
                                } else {
                                    nextInt().toString()
                                }
                            }
                            1 -> feedRemoteId = nextNonEmptyString()
                            2 -> title = nextNonEmptyString()
                            3 -> author = nextNullableString()
                            4 -> content = nextNullableString()
                            5 -> link = nextNullableString()
                            6 -> isRead = nextInt().toBoolean()
                            7 -> isStarred = nextInt().toBoolean()
                            8 -> pubDate = DateUtils.fromEpochSeconds(nextLong())
                            else -> skipValue()
                        }
                    }
                }

                items += item
                endObject()
            }

            endArray()

            while (peek() != JsonReader.Token.END_OBJECT) {
                skipField()
            }

            endObject()

            items
        } catch (e: Exception) {
            throw ParseException(e.message)
        }
    }

    companion object {
        val NAMES: JsonReader.Options = JsonReader.Options.of(
            "id", "feed_id", "title", "author", "html", "url",
            "is_read", "is_saved", "created_on_time"
        )
    }
}