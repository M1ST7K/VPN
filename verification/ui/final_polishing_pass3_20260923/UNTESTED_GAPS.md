# Untested gaps — Final Polishing Pass 3

- Real Disconnect / Cancel taps on a live VPN session: NOT EXECUTED (no runtime subscription;
  06/07 frames are debug visual state only). CTA centering for those labels is measured on render.
- Error state CTA frame: NOT CAPTURED (no real failing session); shares the same view and centering path.
- 04 «Вставить» (Paste): NOT EXECUTED — adb cannot write the API 34 clipboard.
- Tablet breakpoints: not recaptured in this pass.
- VPN E2E (TUN → HEV → Xray → server), public IP change, DNS/IPv6: NOT EXECUTED.
- Physical device: NOT EXECUTED.
- RELEASE READY: NO.
