# DEMO SCRIPT

## Pre-requisites
- Local emulator/device running JARVIS RC.

## 1. Context Seeding
- **Action**: Speak "I prefer morning flights."
- **Verification**: Database logs memory insert as `GLOBAL` preference.

## 2. Long Horizon Objective
- **Action**: Speak "Prepare tomorrow's trip to Delhi."
- **Observation**: UI displays a structured Plan (Check Calendar -> Find Flights -> Summarize).

## 3. Policy Execution
- **Action**: DAG dispatches "Find Flights".
- **Observation**: JARVIS executes natively via Driver.
- **Action**: DAG dispatches "Book Flight".
- **Observation**: `PolicyEngine` triggers `WAITING_FOR_USER_CONFIRMATION` UI. Action stalls safely until user manually taps "Authorize".

## 4. Background Continuation
- **Action**: User authorizes.
- **Observation**: JARVIS finalizes booking and stores result in `History`, notifying user of objective completion.
