#!/bin/bash

# @author Michal Karm Babacek

# This simple tool creates an empty Azure Blob Storage Container

container_name="$1"
# Azurite and all testing emulators fixed credentials.
storage_account="devstoreaccount1"
access_key="Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw=="
# Fixed, local testing instance.
blob_store_url="127.0.0.1:10000"
authorization="SharedKey"
request_method="PUT"
request_date=$(TZ=GMT date "+%a, %d %h %Y %H:%M:%S %Z")
storage_service_version="2016-05-31"
x_ms_date_h="x-ms-date:$request_date"
x_ms_version_h="x-ms-version:$storage_service_version"
x_ms_client_request_id_h="x-ms-client-request-id:SinkitTest"
canonicalized_resource="/${storage_account}/${container_name}/${blob_name}"
processed_headers="${x_ms_client_request_id_h}\n${x_ms_date_h}\n${x_ms_version_h}"
string_to_sign="${request_method}\n\n\n\napplication/octet-stream\n\n\n\n\n\n\n${processed_headers}\n${canonicalized_resource}"
decoded_hex_key="`echo -n $access_key | base64 -d -w0 | xxd -p -c256`"
# Good to know that openssl tool takes hex string, but the HMAC lib function takes key in binary :-0
signature=$(printf "$string_to_sign" | openssl dgst -sha256 -mac HMAC -macopt "hexkey:$decoded_hex_key" -binary | base64 -w0)
authorization_header="Authorization: $authorization $storage_account:$signature"
URL="http://${blob_store_url}/${storage_account}/${container_name}?restype=container"
curl -vvv \
  -X "${request_method}" \
  -H "${x_ms_date_h}" \
  -H "${x_ms_version_h}" \
  -H "${x_ms_client_request_id_h}" \
  -H "${authorization_header}" \
"$URL"
