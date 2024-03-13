package com.erdemserhat.models

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(DelicateCoroutinesApi::class)
data class CandidatePasswordResetRequest(
    val code:String,
    val email:String,
    var isExpired:Boolean = false,
    var attempts:Int =0,
    val uniqueId: String = UUID.randomUUID().toString()
){
    init {
        GlobalScope.launch{
            delay(120_000)
                isExpired=true


        }

    }

    fun incrementAttempt(){
        attempts++
    }


}
