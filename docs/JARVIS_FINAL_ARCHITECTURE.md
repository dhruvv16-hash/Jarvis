# JARVIS FINAL ARCHITECTURE

## System Overview
JARVIS acts as an intermediate reasoning broker between user intentions and Android device APIs. It is not an execution shell; it is an intelligent planning substrate utilizing directed acyclic graphs and strict policy limitations.

## The Architecture Graph

                              USER
                               ¦
                       Voice / Text / Event
                               ¦
                               ?
                        +--------------+
                        ¦ AgentRequest ¦
                        +--------------+
                               ¦
                               ?
                       +--------------+
                       ¦ AgentRuntime ¦
                       +--------------+
                              ¦
              +---------------+----------------+
              ?               ?                ?
          OBJECTIVE      PERSONAL CONTEXT    SESSION
              ¦               ¦
              ¦       +-------+--------+
              ¦       ?       ?        ?
              ¦    Memory   Entities  App World
              ¦                         ¦
              ¦                    Procedures
              ¦                         ¦
              ?                         ¦
       STRATEGIC PLANNER                ¦
              ¦                         ¦
              ?                         ¦
          TASK GRAPH                    ¦
              ¦                         ¦
              ?                         ¦
       TACTICAL PLANNER ?---------------+
              ¦
              ?
           TOOL CALL
              ¦
              ?
          VALIDATION
              ¦
              ?
           POLICY
              ¦
              ?
      CAPABILITY ROUTER
              ¦
              ?
      DRIVER SELECTION
              ¦
     +--------+-----------+
     ?        ?           ?
   Native   Intent   Accessibility
                         ¦
                         ?
                   Target Resolver
                         ¦
                  +-------------+
                  ?             ?
              Semantic       Vision
              Matching       Fallback
                  ¦             ¦
                  +-------------+
                         ?
                      ANDROID
                         ¦
                         ?
                    OBSERVATION
                         ¦
                         ?
                    VERIFICATION
                         ¦
              +----------+-----------+
              ?          ?           ?
           HISTORY     LEARNING    TELEMETRY
              ¦          ¦           ¦
              +----------+-----------+
                         ?
                       ROOM
                         ¦
                         ?
                      SQLITE
