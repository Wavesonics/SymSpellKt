package com.darkrockstudios.fdic

sealed class FdicException(msg: String) : Exception(msg)

class FdicFormatException(msg: String) : FdicException(msg)

class FdicValidationException(msg: String) : FdicException(msg)