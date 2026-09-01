# PERFORMANCE REPORT
*Note: Numbers are simulated baseline constraints for local models.*

- **Memory Extraction**: O(1) alias matching, ~15ms query via Room FTS on 5,000 entities.
- **Strategic Planning (DAG)**: < 15ms local DFS validation. External LLM inference bounded entirely by network/device NPU (variable 1.5s - 4s).
- **Driver Dispatch**: < 10ms for intent mapping. Accessibility node traversal averages 120ms per visual frame.
- **Background Startup**: EventDeduplicator consumes < 5ms before triggering Coroutine Dispatchers.
