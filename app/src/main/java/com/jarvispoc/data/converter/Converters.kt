package com.jarvispoc.data.converter

import androidx.room.TypeConverter
import com.jarvispoc.memory.MemoryCategory

class Converters {
    @TypeConverter
    fun fromMemoryCategory(value: MemoryCategory): String {
        return value.name
    }

    @TypeConverter
    fun toMemoryCategory(value: String): MemoryCategory {
        return MemoryCategory.valueOf(value)
    }
}
