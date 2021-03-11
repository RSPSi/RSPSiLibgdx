package com.rspsi.game


fun checkedLight(colour: Int, light: Int): Int {
    var light = light
    if (colour == -2) return 12345678
    if (colour == -1) {
        if (light < 0) {
            light = 0
        } else if (light > 127) {
            light = 127
        }
        return 127 - light
    }
    light = light * (colour and 0x7f) / 128
    if (light < 2) {
        light = 2
    } else if (light > 126) {
        light = 126
    }
    return (colour and 0xff80) + light
}


fun light(colour: Int, light: Int): Int {
    var light = light

    if (colour == -1) return 12345678
    light = light * (colour and 0x7f) / 128
    if (light < 2) {
        light = 2
    } else if (light > 126) {
        light = 126
    }
    return (colour and 0xff80) + light
}


infix fun Any?.ifNull(block: () -> Unit) {
    if (this == null) block()
}