#!/bin/bash

set -e
set -x

keyType="ed25519"

keySize=2048
days=10000
password="password"
encryptionAlgo="aes-256-cbc"


workDir="target/openssl-work"
destinationDir="target/classes/edDSA"

# see https://stackoverflow.com/a/54924640
export MSYS_NO_PATHCONV=1

if [[ -n "${JAVA_HOME}" ]] ; then
  keytool="$JAVA_HOME/bin/keytool"
elif ! [[ -x "$(command -v keytool)" ]] ; then
  echo 'Error: Either add keytool to PATH or set JAVA_HOME' >&2
  exit 1
else
  keytool="keytool"
fi

if ! [[ -x "$(command -v openssl)" ]] ; then
  echo 'Error: openssl is not installed.' >&2
  exit 1
fi

mkdir -p "$workDir"
mkdir -p "$destinationDir"

# Ed25519  private key
#openssl genpkey -algorithm ed25519   -out "$destinationDir/key_ed25519.pem"
ssh-keygen -t ed25519 -o -a 100 -N "" -f "$destinationDir/key_ed25519.pem" -C "test@localhost"


# Ed25519  public key
#openssl pkey -in "$destinationDir/key_ed25519.pem" -pubout -out "$destinationDir/key_ed25519.pem.pub"
ssh-keygen -y -f "$destinationDir/key_ed25519.pem" > "$destinationDir/key_ed25519.pem.pub"

#generate known-hosts
echo -n "127.0.0.1 $(sed 's/\(.*\) \([^ ]*\)$/\1/' "$destinationDir/key_ed25519.pem.pub")" >> "$destinationDir/known_hosts_eddsa"
#echo -n "127.0.0.1 ssh-ed25519 $(sed -e '1d' -e '$d' "$destinationDir/key_ed25519.pem.pub")" >> "$destinationDir/known_hosts_eddsa"


