# TEH-Link Secure Session (Firmware — Xibalba)

Companion Android implementation: `TehLinkSecureSession.kt` + `TehLinkCrypto.kt`.

## Handshake command

App → device:

```json
{"cmd":"secure_handshake","id":1,"client_pubkey":"<base64 SPKI P-256>"}
```

Device → app:

```json
{"ok":true,"id":1,"data":{"server_pubkey":"<base64>","salt":"<base64>","session_id":"<uuid>"}}
```

## Key derivation

1. ECDH P-256 (`secp256r1`) shared secret.
2. HKDF-SHA256 with salt from `data.salt` (or `session_id` bytes).
3. Info string: `teh-link-v3`.
4. Output: 32-byte AES-256 key.

## Encrypted payloads

After handshake, lines may be wrapped:

```json
{"enc":true,"nonce":"<b64 12 bytes>","ciphertext":"<b64 AES-GCM>"}
```

Plaintext inside ciphertext is standard TEH-Link NDJSON.

## ESP32 reference (mbedTLS / Arduino)

```cpp
// teh_secure.cpp — register in TEH-Link command table
#include "mbedtls/ecdh.h"
#include "mbedtls/gcm.h"

static uint8_t session_key[32];
static bool session_ready = false;

// On secure_handshake: parse client_pubkey, generate ephemeral EC key,
// ECDH → HKDF → session_key, respond with server_pubkey + salt.
// On inbound/outbound: if session_ready, AES-GCM encrypt/decrypt JSON lines.
```

## Public commands (no auth)

`ping`, `get_info`, `pair`, `secure_handshake` remain in `PUBLIC_CMDS` on both sides.

## Rollback

If firmware lacks `secure_handshake`, Android falls back to token auth only (`auth` field).
