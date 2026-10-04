# Kaggle Controller: quick user guide

## 1. Connect
1. On kaggle.com open **Settings -> API -> Generate New Token**.
2. Copy the token (starts with `KGAT_`).
3. Paste it in the app and tap **Verify and connect**.

## 2. Make or open a notebook
- **Notebooks tab -> New notebook** creates a draft **on your phone** (nothing goes to Kaggle yet).
- The file button at the top imports `.ipynb`, `.py`, `.r`, `.rmd`.
- Long-press a notebook -> **Download to editor** to pull it from Kaggle as a draft.

## 3. Edit
- Tap a cell to edit it. The cell menu (three dots) adds, duplicates, moves, converts or deletes cells.
- The key bar above the keyboard has TAB, INDENT, brackets, copy/paste, undo/redo and RUN.
- Search icon = find/replace in the current cell. Three-dot menu = notebook settings (GPU, internet, inputs) and snippets.
- Drafts autosave. The label under the title tells you: Saved locally, Syncing, Synced, Unsaved changes, Sync failed.

## 4. Run
- Tap **RUN**, read the confirmation, then **Save and run**. This saves a new version on Kaggle and starts it.
- The run screen shows status and the log (refreshes every few seconds, slower over time, stops when done).
- Output files have a download button. You get a notification when the run finishes (if allowed).

## 5. Things that open Kaggle in your browser
Cancel a run, server schedules, secrets, version lists. Kaggle's public API doesn't offer them yet, so the app says so and gives you **Open in Kaggle** instead of faking it.

## 6. Privacy
No server of ours. Your token is encrypted on the phone and only sent to Kaggle. Sign out removes it.
