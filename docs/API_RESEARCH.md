# Kaggle API research (done before any code was written)

Date of research: 3 Oct 2026. Rule for this project: **never invent an endpoint.** Everything the app
calls is listed here with where the evidence came from. Anything not listed is "Open in Kaggle".

## 1. Authentication

- Kaggle's current docs say you create an API token at <https://www.kaggle.com/settings/api>
  ("Generate New Token"). The token starts with `KGAT_`. (Source: Kaggle CLI docs README, Kaggle MCP docs.)
- Kaggle's own tooling accepts it as an environment variable `KAGGLE_API_TOKEN` or a file
  `~/.kaggle/access_token`. The older `kaggle.json` username+key pair is still offered as "Legacy API Credentials".
- Kaggle also documents OAuth 2.0 (`/api/v1/oauth2/authorize` and `/token`) and a `kaggle auth login` flow.
  OAuth clients need registration; a personal token is the simplest path for a phone app, so **v0.1 uses the
  personal token only.** OAuth is a future option.
- The token is sent as `Authorization: Bearer KGAT_...`.

## 2. Protocol

The official Python SDK (`kagglesdk`) talks to a Connect-style JSON-RPC API:

    POST https://api.kaggle.com/v1/{service}/{method}
    Content-Type: application/json
    Authorization: Bearer KGAT_...

An independent Go port (`danny.vn/kaggle`, May 2026) states each call it implements was validated against
the live API. Its method list is where the verified kernel calls below come from.

## 3. Calls the app makes

| Feature | Service / method | Evidence |
|---|---|---|
| Who am I (verify token) | `security.OAuthService/IntrospectToken` | kagglesdk source, Go port `WhoAmI` |
| List/search notebooks | `kernels.KernelsApiService/ListKernels` (search, user, language, pageSize) | third-party gateway docs; CLI `kernels list` options |
| Pull notebook source | `kernels.KernelsApiService/GetKernel` (userName, kernelSlug, versionLabel) | kaggle-cli issue #1200 |
| Push / save / **run** | `kernels.KernelsApiService/SaveKernel` | Go port; `kernelExecutionType` = `SAVE_AND_RUN_ALL` or `QUICK_SAVE` |
| Run status | `kernels.KernelsApiService/GetKernelSessionStatus` | Go port; values QUEUED, RUNNING, COMPLETE, ERROR, CANCEL_REQUESTED, CANCEL_ACKNOWLEDGED, NEW_SCRIPT |
| Output files + log | `kernels.KernelsApiService/ListKernelSessionOutput` (pageToken, versionLabel) | Go port: returns signed file URLs and a `log` string |
| Delete notebook | `kernels.KernelsApiService/DeleteKernel` | Go port |
| List datasets / models / competitions | `datasets.DatasetApiService/ListDatasets`, `models.ModelApiService/ListModels`, `competitions.CompetitionApiService/ListCompetitions` | gateway docs |

Important facts learned:

- **There is no separate "run" call.** Saving a kernel triggers the run (CLI: "Pushes ... then runs the kernel";
  `--no-run` = Quick Save). The app maps RUN to `SAVE_AND_RUN_ALL` and "Save to Kaggle" to `QUICK_SAVE`.
- A `SaveKernel` HTTP 200 can still be a rejection: check `error` and the `invalid*Sources` lists. The app does.
- Output files come as **signed URLs fetched without the Authorization header**.
- The log arrives as a snapshot string inside `ListKernelSessionOutput`. No documented streaming endpoint, so the
  monitor polls with exponential backoff and stops when the run ends.
- Accelerators (CLI docs, "as of Sep 2026"): NvidiaTeslaT4 (T4 x2), NvidiaTeslaA100, NvidiaL4, TpuV5E8,
  NvidiaL4X1, TpuV6E8, NvidiaH100, NvidiaRtxPro6000. Some are competition- or admin-only, so the app offers only
  CPU, T4 x2, L4 and TPU v5e-8 and lets Kaggle accept or refuse.

## 4. Things Kaggle does NOT expose publicly (so the app uses Open in Kaggle)

- Cancel a running notebook (status values exist, no documented call).
- Server-side notebook schedules (not in the CLI/SDK).
- Notebook Secrets / Add-ons ("no CLI support", per Kaggle's docs).
- A notebook version list and a run-history list (only `versionLabel` lookups are documented).
- Forum posting. The CLI documents read-only forum browsing.

## 5. Known uncertainty (read this)

I could not call the live API from my sandbox, so response field names for `ListKernels`, `ListDatasets`,
`ListModels`, `ListCompetitions` and `GetKernel` are **read defensively** (several candidate keys, nothing
fabricated when missing). If a list shows blank titles, the fix is one place: `data/kaggle/KaggleRepositories.kt`.
Everything is also tracked in `core/common/Capabilities.kt`.

## Sources

- Kaggle CLI docs: github.com/Kaggle/kaggle-cli/blob/main/docs/README.md and docs/kernels.md
- Kaggle MCP docs: kaggle.com/docs/mcp (KGAT bearer token)
- Kaggle public API docs: kaggle.com/docs/api (OAuth endpoints)
- Go port with live-validated kernels/datasets RPCs: pkg.go.dev/danny.vn/kaggle
- kaggle-cli issue #1200 (GetKernel / ListKernelSessionOutput `version_label`)
- Third-party gateway reference listing ListKernels/ListDatasets/ListModels/ListCompetitions
