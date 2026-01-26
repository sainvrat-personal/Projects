# Project Initialization Steps: Creating Tasks with Taskmaster

This guide outlines the general steps to initialize a project and create actionable tasks using Taskmaster (`tm`). Update this file as new best practices emerge or workflows change.

---

## 1. Initialize Taskmaster in Your Project
- Run the initialization command in your project root:
  ```bash
  tm init --name "<Project Name>" --description "<Brief Description>" --version "0.1.0" -y
  ```
- This sets up Taskmaster configuration and directories.

## 2. Prepare a Product Requirements Document (PRD)
- Draft a PRD describing the project's goals, features, and requirements.
- Use the template at `.taskmaster/templates/example_prd.txt` for reference.
- Save your PRD in `.taskmaster/docs/` (e.g., `.taskmaster/docs/project-prd.txt`).

## 3. Parse the PRD to Generate Tasks
- Run the following command to create tasks from your PRD:
  ```bash
  tm parse-prd .taskmaster/docs/project-prd.txt
  ```
- This generates a `tasks.json` file with structured tasks.

## 4. List and Review Generated Tasks
- View all tasks and subtasks:
  ```bash
  tm list --with-subtasks
  ```
- Review each task for clarity, dependencies, and priority.

## 5. Refine and Expand Tasks
- For complex tasks, break them down into subtasks:
  ```bash
  tm expand --id <taskId> --force
  ```
- Use the complexity analysis tool to identify tasks needing further breakdown:
  ```bash
  tm analyze-complexity --research
  tm complexity-report
  ```

## 6. Update Tasks as Needed
- Add, edit, or remove tasks using Taskmaster commands:
  - Add a new task:
    ```bash
    tm add-task -p "Describe the new task here"
    ```
  - Update a task:
    ```bash
    tm update-task --id <taskId> -p "New details or changes"
    ```
  - Remove a task:
    ```bash
    tm remove-task --id <taskId> -y
    ```

## 7. Manage Task Status and Dependencies
- Set task status as work progresses:
  ```bash
  tm set-status --id <taskId> --status done
  ```
- Add dependencies between tasks:
  ```bash
  tm add-dependency --id <taskId> --depends-on <depId>
  ```

## 8. Use Tags for Advanced Workflows (Optional)
- Create tags for features, experiments, or team collaboration:
  ```bash
  tm add-tag <tagName> --description "Context for this tag"
  tm use-tag <tagName>
  ```
- Parse PRDs into specific tags for isolated task lists.

## 9. Keep This Guide Updated
- Regularly review and update these steps as new Taskmaster features, best practices, or project needs arise.
- Document any custom workflows or patterns discovered during development.

---

**Reference:** See `.github/instructions/dev_workflow.instructions.md` and `.github/instructions/taskmaster.instructions.md` for deeper details and advanced usage.
