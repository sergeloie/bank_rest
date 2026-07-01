---
name: amplicode-mcp
description: Use when you need to interact with the Amplicode MCP server for project analysis, build operations, Spring beans inspection, endpoints discovery, entity details, or debugging. Trigger on requests involving project structure, build status, Spring context, or running/debugging the application.
---

# Amplicode MCP Integration

MCP server is running at `http://127.0.0.1:64442/sse`.

## How to call

Use the `mcp` tool (`.mimocode/tools/mcp.ts`):

```
mcp(tool="tool_name", arguments={...})
```

## Protocol

1. GET `/sse` → receive `event: endpoint\ndata: /message?sessionId=<UUID>`
2. POST `/message?sessionId=<UUID>` with JSON-RPC body
3. Read SSE stream for `event: message\ndata: <JSON response>`

## Available tools (51 total)

### Project Analysis
| Tool | Description |
|------|-------------|
| `get_project_summary` | Language, Spring Boot version, modules |
| `list_module_dependencies` | Module dependencies from build files |
| `refresh_build_system_model` | Refresh after build file changes |

### Build & Analysis
| Tool | Description |
|------|-------------|
| `rebuild_project` | Build project, get compilation errors |
| `analyze_files` | Inspections for errors/warnings |

### Source Code
| Tool | Description |
|------|-------------|
| `read_class_file` | Read class source by FQN (bodies excluded by default) |
| `list_runnables` | Find main/test/Spring Boot entry points |
| `run_runnable` | Execute a runnable element |

### Spring Context
| Tool | Description |
|------|-------------|
| `list_spring_bean` | List Controllers, Services, Repos, Configs |
| `get_bean_injection_info` | DI graph for a bean |
| `get_properties_values` | Read Spring property value |
| `list_application_properties_files` | Find properties/yaml files |
| `list_spring_profiles` | List active profiles |

### Domain Model
| Tool | Description |
|------|-------------|
| `list_all_domain_entities` | All JPA/JDBC entities |
| `get_entity_details` | JPA entity fields, relations, annotations |
| `get_jdbc_entity_details` | JDBC entity details |
| `list_entity_dtos` | DTOs for an entity |
| `list_entity_mappers` | Mappers (MapStruct etc.) |
| `list_entity_repositories` | Repositories for an entity |

### API & Security
| Tool | Description |
|------|-------------|
| `list_project_endpoints` | All REST endpoints |
| `get_endpoint_info` | Details for a specific endpoint |
| `list_security_configurations` | Security config classes |
| `list_spring_security_roles` | Defined roles |

### Infrastructure
| Tool | Description |
|------|-------------|
| `list_db_migration_files` | Flyway/Liquibase migrations |
| `create_migration_script` | Generate a migration script |
| `list_project_datasources` | DataSource configurations |
| `list_docker_compose_files` | Docker Compose files |

### Kafka
| Tool | Description |
|------|-------------|
| `list_kafka_consumers` | @KafkaListener consumers |
| `list_kafka_producers` | KafkaTemplate producers |

### Testing
| Tool | Description |
|------|-------------|
| `list_test_files` | Find test files |
| `run_tests` | Execute tests |

### Connekt
| Tool | Description |
|------|-------------|
| `list_connekt_files` | Find .connekt.kts files |

### Debugging
| Tool | Description |
|------|-------------|
| `add_breakpoint` | Add a breakpoint |
| `list_breakpoints` | List breakpoints |
| `remove_breakpoint` | Remove a breakpoint |
| `set_breakpoint_condition` | Set conditional breakpoint |
| `toggle_breakpoint` | Enable/disable breakpoint |
| `debug_run_configuration` | Start debug session |
| `list_debug_sessions` | List active debug sessions |
| `stop_debug_session` | Stop a debug session |
| `pause` | Pause execution |
| `resume` | Resume execution |
| `step_into` | Step into method |
| `step_out` | Step out of method |
| `step_over` | Step over line |
| `evaluate_expression` | Evaluate expression in debugger |
| `get_current_position` | Current execution position |
| `get_stack_trace` | Current stack trace |
| `list_threads` | List threads |
| `get_console_output` | Get process console output |
| `list_run_configurations` | List run configurations |

## Notes

- Most tools accept `projectPath` and `moduleName` arguments
- Pass `projectPath` = project root when known
- Module name can be obtained from `get_project_summary`
