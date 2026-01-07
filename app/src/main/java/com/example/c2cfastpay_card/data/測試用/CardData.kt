package com.example.c2cfastpay_card.data

import androidx.annotation.DrawableRes

data class CardData(
    val id: Int,
    val title: String,
    val description: String,
    @DrawableRes val image: Int,
)
