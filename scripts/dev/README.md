# Local Dev Scripts

## Session Smoke

`Invoke-LocalSessionSmoke.ps1` runs a real HTTP smoke flow against a running `test` profile app.

It checks:

- anonymous protected route redirect
- demo login (`d / d`)
- signup auto-login
- signup recovery email confirm via development preview link
- profile update and session refresh
- password change and fresh login with the new password
- account deletion request and forced logout

Example:

```powershell
.\scripts\dev\Invoke-LocalSessionSmoke.ps1 -BaseUrl http://localhost:18080
```

The script writes a markdown report under `build/local-smoke/reports/`.
