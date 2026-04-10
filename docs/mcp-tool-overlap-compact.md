# MCP Tool Overlap (Compact, Unified)

Legend: `JC` = jCodemunch, `JB-Idx` = JetBrains index tools, `JB-M` = JetBrains MCP tools

## Environment / Readiness
- **Repo or IDE readiness**
  - `JC`: `resolve_repo`, `list_repos`
  - `JB-M`: `list_projects`
  - `JB-Idx`: `ide_index_status`
  - Optional companion: `jetbrains-get_mcp_companion_overview`
- **Overlap**: Partial

## Project Overview / Structure
- **High-level project shape**
  - `JC`: `get_repo_outline`, `get_file_tree`, `suggest_queries`, `get_repo_health`
  - `JB-Idx`: `ide_find_file` (discovery-oriented)
- **Overlap**: Partial

## File Discovery
- **Find files by name/pattern**
  - `JC`: `get_file_tree`, `search_text`
  - `JB-Idx`: `ide_find_file`
- **Overlap**: True

## Symbol / Function Discovery
- **Find classes, methods, functions, types**
  - `JC`: `search_symbols`, `get_file_outline`
  - `JB-Idx`: `ide_find_class`, `ide_find_definition`
  - `JB-M`: `find_symbol`, `get_file_symbols`, `get_symbol_info`
- **Overlap**: True

## Symbol Source / Context
- **Read source and context around symbols**
  - `JC`: `get_symbol_source`, `get_context_bundle`, `get_ranked_context`, `get_file_content`
  - `JB-Idx`: `ide_find_definition` (preview/full element)
  - `JB-M`: `get_symbol_info`, `get_file_symbols`
- **Overlap**: Partial

## References / Usages
- **Find where symbol is used**
  - `JC`: `find_references`, `check_references`
  - `JB-Idx`: `ide_find_references`
  - `JB-M`: `find_references`
- **Overlap**: True

## Implementations / Hierarchies / Calls
- **Implementations**
  - `JB-Idx`: `ide_find_implementations`
- **Type hierarchy**
  - `JC`: `get_class_hierarchy`
  - `JB-Idx`: `ide_type_hierarchy`
  - `JB-M`: `get_type_hierarchy`
- **Call hierarchy**
  - `JC`: `get_call_hierarchy`
  - `JB-Idx`: `ide_call_hierarchy`
- **Super methods**
  - `JB-Idx`: `ide_find_super_methods`
- **Overlap**: Partial to True (depends on subtask)

## Dependency / Impact / Blast Radius
- **What breaks if this changes**
  - `JC`: `get_blast_radius`, `get_impact_preview`, `get_dependency_graph`, `find_importers`
- **Overlap**: Mostly unique (`JC`)

## Architecture / Risk / Dead Code
- **Repo-scale architecture and risk analytics**
  - `JC`: `find_dead_code`, `get_dead_code_v2`, `get_dependency_cycles`, `get_coupling_metrics`, `get_layer_violations`, `get_hotspots`, `get_churn_rate`, `get_symbol_importance`, `get_extraction_candidates`
- **Overlap**: Unique (`JC`)

## Refactor / Safe Mutation
- **Rename, move, safe delete**
  - `JC`: `check_rename_safe` (preflight analysis)
  - `JB-Idx`: `ide_refactor_rename`, `ide_move_file`, `ide_refactor_safe_delete`
  - `JB-M`: `refactor_rename`, `move_file`, `refactor_safe_delete`
- **Overlap**: True for capability; JetBrains strongest for execution

## Diagnostics / Build / Tests
- **Compiler/test diagnostics and run configs**
  - `JB-Idx`: `ide_diagnostics`
  - `JB-M`: `list_run_configurations`, `run_configuration`, `get_test_results`
- **Overlap**: Partial

## Freshness / Index Lifecycle
- **Keep indexes and analysis fresh**
  - `JC`: `register_edit`, `index_file`, `index_folder`, `invalidate_cache`
  - `JB-Idx`: `ide_sync_files`, `ide_index_status`
- **Overlap**: Partial

## Session / Productivity Meta
- **Session context and planning aids**
  - `JC`: `get_session_context`, `get_session_snapshot`, `get_session_stats`, `plan_turn`
- **Overlap**: Unique (`JC`)

## Quick Same-Intent Equivalents
- **References**: `JC.find_references` ↔ `JB-Idx.ide_find_references` ↔ `JB-M.find_references`
- **Type hierarchy**: `JC.get_class_hierarchy/get_type_hierarchy` ↔ `JB-Idx.ide_type_hierarchy` ↔ `JB-M.get_type_hierarchy`
- **Symbol discovery**: `JC.search_symbols` ↔ `JB-Idx.ide_find_class/ide_find_definition` ↔ `JB-M.find_symbol/get_file_symbols`
- **Refactor safety**: `JC.check_rename_safe` ↔ `JB-Idx.ide_refactor_rename/ide_refactor_safe_delete` ↔ `JB-M.refactor_rename/refactor_safe_delete`
- **Freshness**: `JC.register_edit/index_file` ↔ `JB-Idx.ide_sync_files/ide_index_status`
