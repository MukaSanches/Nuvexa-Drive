# Nuvexa Managed Mode

Nuvexa Managed Mode removes the inherited server-address/provider onboarding from the normal first-run experience.

## Production

The production `generic` flavor keeps managed provisioning disabled until a permanent Nuvexa-operated backend is deployed and validated. A public demo service must never be used as durable customer storage.

The repository includes a Nuvexa server scaffold under `.nuvexa/server/` for the permanent backend path.

## QA Auto Setup

The `qa` flavor enables a zero-login test path against the official temporary Nextcloud trial service.

Flow:

1. Nuvexa splash starts with no account.
2. The app requests a temporary trial from `https://try.nextcloud.com/`.
3. The response is restricted to HTTPS hosts matching `demo[0-9]+.nextcloud.com`.
4. The existing, mature account creation path receives the temporary server/user credential through Nuvexa's direct-login URI.
5. The inherited server URL, provider registration and QR setup controls stay hidden.
6. The app opens directly into the file experience after authentication completes.

The public trial account is temporary and is for QA only. Files placed there are not durable and may disappear when the trial expires.

No shared Nuvexa password, provisioning secret or permanent production credential is embedded in the APK.
