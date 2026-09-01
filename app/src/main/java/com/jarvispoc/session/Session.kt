package com.jarvispoc.session

import com.jarvispoc.agent.Goal

data class Session(val id: String, val activeGoal: Goal?)
