#!/bin/bash
# This script ensures the same debug keystore is used for every build
# It creates/updates the keystore with identical credentials

KEYSTORE_PATH="debug.keystore"
ALIAS="kaggle-debug"
PASSWORD="kaggle-debug"
STOREPASS="kaggle-debug"

echo "Ensuring forever-use debug keystore at $KEYSTORE_PATH..."

if command -v keytool >/dev/null 2>&1; then
    keytool -genkey -v -keystore "$KEYSTORE_PATH" -alias "$ALIAS" -keyalg RSA -keysize 2048 -validity 10000 -storepass "$STOREPASS" -keypass "$PASSWORD" -dname "CN=Kaggle Controller, OU=Dev, O=Kaggle, L=City, ST=State, C=US" -noprompt 2>/dev/null || true
    echo "Keystore created/replaced with consistent debug credentials."
else
    echo "keytool not available in this environment (available in GitHub Actions runners)."
fi
