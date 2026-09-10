# HotFox bootstrap payload

`bootstrap/payload/part-*.b64` are ordered text chunks of a verified XZ payload. `bootstrap/bootstrap_source.sh` concatenates and verifies them before extracting the HotFox 2.1.0 source overlay, Ultra Master Prompt and UI reference.

Do not edit chunk contents manually. The expected reconstructed payload SHA-256 is:

`c9dd4656985b73084e3ff9bf16459c93d55e0a2cfb0ce8dd083ac1d1dea85260`
