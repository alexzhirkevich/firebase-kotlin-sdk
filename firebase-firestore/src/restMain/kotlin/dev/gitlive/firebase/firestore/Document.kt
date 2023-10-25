package dev.gitlive.firebase.firestore

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
internal class Document(
    val name : String?=null,
    val fields: JsonObject,
    val createTime : String? = null,
    val updateTime : String? = null,
)