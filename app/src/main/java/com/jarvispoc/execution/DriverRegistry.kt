package com.jarvispoc.execution

class DriverRegistry {
    private val drivers = mutableMapOf<DriverType, ExecutionDriver>()
    fun register(driver: ExecutionDriver) { drivers[driver.type] = driver }
    fun getDriver(type: DriverType): ExecutionDriver? = drivers[type]
    fun getAllDrivers(): List<ExecutionDriver> = drivers.values.toList()
}
