package dev.gitlive.firebase.firestore

import kotlin.random.Random

const val AUTO_ID_LENGTH = 20
private const val AUTO_ID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

object AutoId {

    private val random = Random // TODO: secure random

    fun next() : String = buildString {
        repeat(AUTO_ID_LENGTH){
            append(AUTO_ID_ALPHABET.random(random))
        }
    }
}