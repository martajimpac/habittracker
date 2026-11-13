---

name: "spec-driven-development"
description: "Use this skill when developing a new feature."
------------------------------------------------------------

# Spec-Driven Development (SDD)

## Purpose

Ensure every non-trivial development task follows a traceable, approval-gated workflow from requirements to verified implementation.

**This skill governs the development process, sequencing, and approval gates. It does not replace technology-specific skills, repository conventions, or mandatory MobiAI workflows.**

For every non-trivial development task, follow this skill strictly.

Never skip a stage, combine approval gates, infer approval from silence, or begin implementation without explicit authorization.

## User Notification — Mandatory

Whenever this skill is invoked for a task, you MUST immediately notify the user that the `spec-driven-development` skill is being used.

The notification must be sent before performing any further task-specific investigation, planning, or implementation.

Use this message:

"I'm using the `spec-driven-development` skill to guide this task through the required specification, planning, approval, implementation, and validation stages."

Requirements:

* Notify the user every time this skill is invoked for a new task.
* Do not wait until the specification or technical plan is ready.
* Do not assume that invoking the skill silently is sufficient.
* Do not claim to have invoked the skill unless it has actually been loaded or invoked.
* If the skill cannot be loaded, explain that the required workflow could not be initialized and report the blocker instead of claiming the skill is being used.
* The notification does not constitute approval of the specification, approval of the technical plan, or authorization to implement.

## 1. Task Classification

First, determine whether the task is trivial or non-trivial.

### Trivial tasks

A task may use a lightweight workflow only when it is clearly limited to a change that does not affect product behavior, architecture, persistence, networking, navigation, or user-visible functionality.

Examples may include:

* Correcting a typo in a comment.
* Fixing an obvious spelling mistake in documentation.
* Formatting a document without changing its meaning.

If a seemingly trivial change reveals broader implications, reassess its classification and follow the full SDD workflow when necessary.

### Non-trivial tasks

The full SDD workflow is mandatory for tasks involving:

* New features or changes to existing functionality.
* Bug fixes that affect application behavior.
* UI, navigation, or interaction changes.
* Data models, persistence, synchronization, or networking.
* Architecture, dependencies, or cross-component behavior.
* Performance, reliability, security, or permissions.
* Changes requiring meaningful acceptance criteria or multiple implementation steps.

**When uncertain, classify the task as non-trivial and use the full workflow.**

Do not bypass SDD merely because the user describes a task as quick, simple, or urgent.

## 2. Mandatory Generic Rules

Before planning, investigating, reviewing, or modifying any project, you MUST read `references/GENERIC_RULES.md` in full and apply all relevant rules throughout the task.

### Required procedure

1. Locate the `references/` directory relative to the directory containing this `SKILL.md` file.
2. Locate `references/GENERIC_RULES.md` inside that directory.
3. Read the complete contents using the available repository tools.
4. Verify that the complete document has been accessed and reviewed.
5. Identify the rules relevant to the current task.
6. Apply those rules during investigation, specification, technical planning, implementation, code review, testing, and validation, as applicable.
7. Revisit the document when necessary to resolve questions or verify compliance.

Do not assume that linked Markdown files are automatically imported, that their contents have already been read, or that a reference to a document constitutes compliance with its requirements.

### Rules and precedence

* Follow `references/GENERIC_RULES.md` alongside this SDD workflow, mandatory MobiAI workflows, technology-specific skills, and repository conventions.
* Generic rules establish reusable working defaults.
* Project-specific conventions may refine generic defaults when they do not conflict with higher-priority instructions or alter required behavior and scope without resolution.
* This SDD skill governs development stages, sequencing, and approval gates.
* `references/MOBILE_GUIDELINES.md` governs applicable mobile-specific requirements and quality standards.
* Applicable technology-specific skills govern their respective technical areas.

If rules conflict in a way that affects behavior, scope, security, architecture, or mandatory workflow, identify the conflict and resolve it before proceeding with the affected work. Never silently ignore a relevant rule or invent a precedence rule to bypass a requirement.

### Failure handling

If `references/GENERIC_RULES.md` is missing, inaccessible, or cannot be read completely:

1. Stop the affected task.
2. Report the specific blocker.
3. Explain what is needed to continue.

Do not claim to have read, understood, or followed the generic rules unless their contents have actually been accessed and reviewed.

## 3. MobiAI Brain — Mandatory Initialization

Before preparing the specification, planning the implementation, investigating project code, reviewing implementation details, or modifying project files:

1. Initialize MobiAI Brain using its supported workflow.
2. Verify that initialization succeeded and Brain is accessible.
3. Query Brain for relevant knowledge, architectural conventions, previous decisions, and related implementation history.
4. Consider the retrieved knowledge when preparing the specification and technical plan.

Complete the mandatory Generic Rules prerequisite in Section 2 before initializing Brain.

If Brain cannot be initialized or accessed, STOP the affected task. Report the failure and what prevented initialization.

Never claim initialization or a query succeeded without verifying the result.

## 4. Mandatory Skill and Subagent Discovery

Before preparing the specification:

1. Confirm that the Generic Rules and MobiAI Brain prerequisites have been completed.
2. Inspect available skills and subagents using the supported discovery mechanisms.
3. Read the `using-mobiai` skill in full.
4. Classify the task and identify all applicable mandatory skills.
5. Read the complete `SKILL.md` of every applicable skill before performing the work it governs.
6. Inspect available subagents and determine whether specialized delegation would be useful.

Do not assume a skill has been loaded merely because its name is known or mentioned in another document.

### Mandatory Android skill routing

* `mobiai-android-arquitecture`: Mandatory for all Android development tasks.
* `android-jetpack-compose`: Mandatory when creating or modifying Jetpack Compose UI.
* `edge-to-edge`: Mandatory when creating or modifying Android screens or UI components.
* `one-shot-ui-events`: Mandatory when implementing or modifying one-shot UI events, including navigation, dialogs, toasts, and snackbars.
* `mobiai-mobile-verification`: Mandatory before declaring an Android task complete.

Preserve the exact skill identifiers used by the repository. Do not assume a similarly named skill is equivalent.

Reassess skill requirements whenever the task scope changes.

If a mandatory skill cannot be accessed, stop the affected work and report the blocker. Do not silently replace missing instructions with assumptions.

### Subagent usage

Use relevant subagents when they are available and appropriate.

For each delegation:

* Define a bounded responsibility.
* Ensure the work does not bypass SDD approval gates.
* Avoid parallel implementation that exceeds the approved scope.
* Integrate and verify delegated results before accepting them.
* Provide the delegated agent with the applicable Generic Rules and relevant instructions, or ensure it can access them.
* Do not allow delegated work to bypass mandatory prerequisites or approval requirements.

If no suitable subagent exists, proceed without delegation and report this limitation.

## 5. Pre-Implementation Report

Before creating or modifying feature documents, provide a concise report containing:

* **Generic Rules:** Whether `references/GENERIC_RULES.md` was read in full, the relevant requirements identified, and any conflicts found.
* **Brain:** Initialization, accessibility, and query status.
* **Task:** Classification, scope, and applicable workflow.
* **Skills:** Applicable mandatory skills and their loading status.
* **Subagents:** Availability, proposed delegation, and reasons.
* **SDD status:** Existing feature documents, approval states, and the next permitted stage.

Report actual outcomes only.

Do not claim that a document was read, a skill was loaded, a subagent was inspected, or a Brain operation was completed unless the action actually occurred and its result was verified.

If a mandatory prerequisite fails, report the blocker and stop the affected work.

## 6. Authoritative SDD Documents

Follow the repository's SDD process and preserve its existing architecture and conventions.

Before using the reference documents, read them in full.

### Reference documents

All reference documents listed below are stored in the `references/` directory located alongside this skill's `SKILL.md` file.

Resolve these paths relative to the skill directory, not relative to the project's root directory or the current working directory.

* `references/SPEC_TEMPLATE.md`: Authoritative specification template, including required sections, `RF-` requirement identifiers, `CA-` acceptance-criteria identifiers, and approval states.
* `references/PLAN_TEMPLATE.md`: Authoritative technical-plan template, including required sections and approval states.
* `references/MOBILE_GUIDELINES.md`: Guidance for requirements, acceptance criteria, technical planning, and mobile validation.
* `references/GENERIC_RULES.md`: Mandatory generic working rules.

Preserve each template's structure and embedded comments when copying it. Do not rewrite shared templates for an individual feature.

Consult `references/MOBILE_GUIDELINES.md` throughout the specification, planning, and validation stages.

Apply relevant requirements from `references/GENERIC_RULES.md` throughout these stages as well.

Consider relevant concerns, including:

* Lifecycle and state retention.
* Connectivity and persistence.
* UI states and navigation.
* Interruptions and form behavior.
* Screen adaptation and accessibility.
* Permissions and performance.
* Background work and privacy.
* Internationalization.

Apply only relevant considerations. Clarify undefined product behavior with the user instead of inventing requirements.

### Feature document locations

Keep feature documents together:

* `ai-docs/features/<feature-name>/SPEC.md`
* `ai-docs/features/<feature-name>/PLAN.md`
* `ai-docs/features/<feature-name>/TASKS.md`

Reuse an existing feature directory when continuing its work. Do not create duplicate directories or documents unnecessarily.

If the repository uses different established paths, inspect its conventions and follow them unless the user has explicitly required the paths above.

## 7. Stage 1 — Specification

1. Identify the feature scope and relevant existing decisions.
2. Read `references/SPEC_TEMPLATE.md` in full.
3. Apply relevant requirements from `references/GENERIC_RULES.md` and `references/MOBILE_GUIDELINES.md`.
4. Create or update `ai-docs/features/<feature-name>/SPEC.md` using the template.
5. Collaborate with the user section by section, resolving missing or ambiguous product decisions.
6. Define testable requirements and acceptance criteria using the template's `RF-` and `CA-` identifiers.
7. Consider relevant Brain knowledge, repository conventions, and mandatory skill guidance.
8. Present the completed specification for review.

Do not start the technical plan until the user explicitly approves the specification.

### Specification approval rules

* Only the user can approve the specification.
* A complete document is not automatically approved.
* Never change an approval state on the user's behalf.
* Do not infer approval from silence, unrelated messages, or requests that do not clearly approve the specification.

After presenting the specification, STOP.

## 8. Stage 2 — Technical Plan

Begin only after explicit user approval of the specification.

1. Read `references/PLAN_TEMPLATE.md` in full.
2. Create or update `ai-docs/features/<feature-name>/PLAN.md` according to the template.
3. Reference the approved specification's requirement and acceptance-criteria identifiers.
4. Inspect the relevant code, architecture, dependencies, and established conventions.
5. Apply relevant requirements from `references/GENERIC_RULES.md`, `references/MOBILE_GUIDELINES.md`, and the applicable technical skills.
6. Define the implementation approach and affected components.
7. Document relevant dependencies, error handling, testing, and validation methods.
8. Identify technical risks, migration concerns, and unresolved questions.
9. Record whether subagents are needed, their bounded responsibilities, and any delegation limitations.

Resolve routine technical details using the codebase and established conventions. Ask the user about unresolved product decisions that affect behavior.

Present the plan for review.

Do not create `TASKS.md` or begin implementation until the user explicitly approves the plan.

### Technical plan approval rules

* Only the user can approve the plan.
* A completed plan is not automatically approved.
* Never change an approval state on the user's behalf.
* Do not infer approval from silence or from approval of the specification alone.

After presenting the plan, STOP.

## 9. Stage 3 — Task Breakdown

Begin only after explicit user approval of the plan.

Create or update `ai-docs/features/<feature-name>/TASKS.md`, deriving tasks from the approved specification and plan.

Each task must include:

* A unique task identifier.
* A concise, verifiable objective.
* Its implementation scope.
* Dependencies on other tasks.
* The `RF-` and/or `CA-` identifiers it addresses.
* A concrete validation method.
* A status reflecting actual progress.

Tasks must be small, ordered, and independently verifiable where practical.

Do not introduce requirements or expand the scope beyond the approved documents.

Ensure the task breakdown complies with relevant generic rules, mobile guidelines, and applicable technical skills.

Present the task breakdown when appropriate.

**Do not begin implementation until the user explicitly authorizes it.**

Approval of the specification and plan does not itself authorize code changes.

## 10. Stage 4 — Implementation

Implementation may begin only when all three conditions are satisfied:

1. The specification is explicitly approved.
2. The technical plan is explicitly approved.
3. The user has explicitly authorized implementation.

Once authorized:

1. Execute the approved tasks in dependency order.
2. Follow `references/GENERIC_RULES.md`, all applicable skills, the approved plan, and repository conventions.
3. Follow relevant requirements in `references/MOBILE_GUIDELINES.md`.
4. Keep modifications within the agreed scope.
5. Update task statuses as work progresses.
6. Maintain consistency between the specification, plan, tasks, and implementation.
7. Reassess applicable skills if the scope changes.
8. Do not silently introduce new product behavior or expand the approved scope.
9. Verify that delegated work, if any, complies with the same rules and approved scope.

If implementation reveals a requirement gap, contradiction, or missing product decision, stop the affected work. Clarify the behavior and update the relevant documents before continuing.

Routine technical details may be resolved using the existing codebase, established conventions, and applicable skills.

Do not request renewed permission for steps already covered by the user's explicit authorization.

## 11. Stage 5 — Validation and Completion

Before declaring an Android task complete:

1. Read and follow `mobiai-mobile-verification`.
2. Apply all relevant requirements in `references/GENERIC_RULES.md` and `references/MOBILE_GUIDELINES.md`.
3. Execute relevant available tests, builds, static analysis, and other checks.
4. Verify the implementation against the approved acceptance criteria.
5. Record validation results and outstanding checks in `TASKS.md`.
6. Update task statuses to reflect the actual outcome.
7. Report failures, limitations, untested behavior, and remaining work.
8. Evaluate whether useful, reusable knowledge should be contributed to MobiAI Brain using its supported workflow.

Never claim a test, build, validation, or other operation succeeded unless it was actually executed and its result verified.

Distinguish clearly between:

* Implementation completed.
* Validation passed.
* Validation partially completed.
* Validation failed.
* Outstanding work.

Do not report a task as fully validated while required checks remain outstanding.

## 12. Scope Changes and Document Consistency

The specification, plan, tasks, and implementation must remain consistent.

Continue to apply relevant requirements from `references/GENERIC_RULES.md`, `references/MOBILE_GUIDELINES.md`, and applicable skills throughout the task.

If new information requires a change to approved requirements or acceptance criteria:

1. Stop the affected work.
2. Explain the discrepancy and its impact.
3. Collaborate with the user to clarify the intended behavior.
4. Update the relevant specification and obtain explicit approval for the changed specification.
5. Update the plan and obtain explicit approval for the changed plan.
6. Update tasks to reflect the approved changes.
7. Resume only within the resulting authorized scope.

If a newly discovered rule conflicts with an approved decision or the current implementation, do not silently ignore it or change product behavior. Explain the conflict and resolve it through the applicable approval process.

Do not use implementation progress as a reason to bypass the approval gates.

## 13. Non-Negotiable Rules

* No planning, project review, or code modification before reading `references/GENERIC_RULES.md` in full.
* No claiming compliance with Generic Rules without actually accessing and reviewing the complete document.
* No skipping mandatory Brain initialization, skill loading, or required verification.
* No technical plan before specification approval.
* No task breakdown before plan approval.
* No implementation before explicit user authorization.
* No approval inferred from silence or document completeness.
* No silently ignoring relevant generic rules or unresolved conflicts that affect behavior or scope.
* No proceeding with affected work when a mandatory prerequisite is inaccessible or unresolved.
* No invented requirements, architectural decisions, test results, or tool outcomes.
* No undocumented expansion of the approved scope.
* No declaring completion while concealing failed or outstanding validation.
* No claiming that unavailable skills or tools were used.
* No skipping required stages because a task appears urgent.

If a mandatory prerequisite cannot be satisfied, stop the affected work, explain the blocker, and identify what is needed to continue.

The objective is to make every non-trivial change traceable from approved requirements through technical planning and implementation tasks to verified acceptance criteria, while consistently applying generic working rules and project-specific requirements.

## 14. Subagent Compatibility

When working as a subagent on a scoped task, do not repeat the parent agent's bootstrap or skill-routing process if the parent has already completed it and explicitly provided the relevant instructions.

However, this exception does not waive applicable SDD constraints, approval gates, implementation authorization requirements, or validation requirements.

The parent agent must provide:

* The relevant Generic Rules and confirmation that they were read.
* The relevant specification.
* The approved technical plan.
* The task scope.
* Applicable skills and instructions.
* Implementation authorization, when implementation is delegated.

The subagent must apply the provided rules and instructions to its delegated work.

If required context or authorization is missing, stop and report the blocker to the parent agent. Do not infer approval from the existence of a delegated task.

## 15. Generic Rules Reference

The mandatory source of generic working rules is:

`references/GENERIC_RULES.md`

This reference is an explicit reading requirement. It does not imply that the file is automatically imported, loaded, or applied.

The SDD skill defines the development workflow and approval gates. Generic Rules define reusable working practices. Mobile Guidelines define mobile-specific requirements and quality standards. Applicable technology-specific skills provide specialized technical instructions.

Read and apply each relevant source independently. Preserve their distinct responsibilities, resolve material conflicts explicitly, and never use one document as justification for bypassing another mandatory requirement.