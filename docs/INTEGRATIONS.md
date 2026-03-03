# Cross-Feature Integration Design

This document defines the integration flows between Prompts, Collections, ComfyUI, and the upcoming Dataset Collection (Phase 5).

## Prompts ↔ ComfyUI

### Current State

The Prompts tab (`SavedPromptsScreen`) stores prompts extracted from CivitAI model images. Actions: Copy, Export, Save as template, Apply template (variable substitution). No direct connection to ComfyUI.

### Planned Integrations

#### 1. Send to ComfyUI

A saved prompt should be sendable to ComfyUI as a pre-filled positive prompt:

- **Entry point:** Long-press or swipe action on a prompt row → "Send to ComfyUI"
- **Behavior:** Opens `ComfyUIGenerationScreen` with `positivePrompt` pre-filled from the selected prompt
- **Navigation:** `SavedPromptsScreen` → `ComfyUIGenerationRoute(prefillPrompt = prompt.text)`
- **Implementation:** Add `prefillPrompt: String? = null` to `ComfyUIGenerationRoute`, pass to ViewModel on init

#### 2. Auto-save ComfyUI Prompts to History

When a ComfyUI generation completes, the positive prompt should be auto-saved to the Prompts History tab:

- **Trigger:** `ComfyUIHistoryViewModel` receives a new generated image event
- **Behavior:** Calls `SavePromptUseCase` with source = `Generated`, text = `positivePrompt` from image metadata
- **Deduplication:** Skip save if an identical prompt text already exists in History within the last 24h

---

## Collections ↔ Dataset (Phase 5)

### Current State

Collections (`CollectionsScreen`) are groups of favorited models. Dataset Collection (Phase 5) is a separate concept for curating images for AI training.

### Relationship Design

Collections and Datasets are **separate concepts** that can cross-reference each other:

| Concept | Purpose |
|---|---|
| Collection | Group of *models* for reference/bookmarking |
| DatasetCollection | Group of *images* for AI training data curation |

A Collection is **not** automatically a Dataset — the user must explicitly convert or import.

### Planned Integration: "Create Dataset from Collection"

- **Entry point:** Collection detail screen → overflow menu → "Create Dataset from images in this collection"
- **Behavior:** Navigates to `DatasetCreationScreen` with images pre-populated from the collection's favorited model images
- **Note:** Only model images (from Favorites) are included, not the models themselves

### Planned Integration: "Import to Dataset"

- **Entry point:** Dataset creation screen → "Import from Collection"
- **Behavior:** Shows a collection picker; selected collection's images are added to the dataset
- **Advantage:** Keeps Dataset creation workflow self-contained; Collections remain unmodified

---

## Navigation IA (Power User Mode)

### Current Tab Structure

`Search / Collections / Prompts / Settings`

### Proposed Power User Enhancement

When **Power User Mode** is ON, consider adding a **Generate** tab that consolidates:

- Output Gallery (ComfyUI generation history)
- Active generation queue
- Workflow templates

This keeps ComfyUI features discoverable without burying them 3+ levels deep in Settings.

**Decision:** Not implementing the Generate tab now (scope too large). Instead, #336 adds an Output Gallery shortcut directly to Advanced Settings. Revisit in Phase 4/5 when ComfyUI features are more complete.

---

## Phase Backlog

| Integration | Phase | Status |
|---|---|---|
| Send prompt to ComfyUI | Phase 4 | Planned |
| Auto-save ComfyUI prompts to History | Phase 4 | Planned |
| Create Dataset from Collection | Phase 5 | Planned |
| Import Collection into Dataset | Phase 5 | Planned |
| Generate tab in bottom nav | Phase 5 | Under discussion |
