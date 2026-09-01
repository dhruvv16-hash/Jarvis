package com.jarvispoc.apps

data class AppDescriptor(val id: String, val packageNames: List<String>, val name: String, val capabilities: List<AppCapability>)
